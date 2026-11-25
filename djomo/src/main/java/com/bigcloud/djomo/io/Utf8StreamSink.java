/*******************************************************************************
 * Copyright 2022 Alex Vigdor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.bigcloud.djomo.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

import com.bigcloud.djomo.internal.ByteBufferPool;

/**
 * A {@link CharSink} that writes UTF-8 to an {@link OutputStream}.
 *
 * <p>
 * Encoding is lenient: unpaired surrogates are written as 3 bytes rather than
 * dropped or rejected, matching the reader. A high surrogate at the end of a
 * chunk is deferred and combined with the start of the next chunk if it is the
 * pair's low half; otherwise it is emitted in order at the start of the next
 * call.
 *
 * <p>
 * The sink's internal UTF-8 buffer is drawn from a shared buffer pool. It is
 * returned to the pool when {@link #last(int)} completes, so a sink must not be
 * used after {@code last()}; repeated calls to {@code last()} are ignored.
 */
public class Utf8StreamSink implements CharSink {
	private static final int BUF_LEN = 4096;
	private static final int SUPP_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;
	private static final long NON_ASCII_MASK = 0xFF80_FF80_FF80_FF80L;
	private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

	private static final ValueLayout.OfLong LONG_UNALIGNED = ValueLayout.JAVA_LONG_UNALIGNED;

	private final byte[] utf8Buffer = ByteBufferPool.acquire();
	private final MemorySegment utf8Seg = MemorySegment.ofArray(utf8Buffer);
	private final OutputStream out;
	char[] buffer;
	private MemorySegment bufSeg;
	int hi = -1;
	private boolean done;

	public Utf8StreamSink(OutputStream out) {
		this.out = out;
	}

	@Override
	public void buffer(char[] buffer) {
		this.buffer = buffer;
		this.bufSeg = buffer != null ? MemorySegment.ofArray(buffer) : null;
	}

	@Override
	public void next(int len) {
		writeChunk(len, false);
	}

	@Override
	public void last(int len) {
		if (done) {
			return;
		}
		done = true;
		try {
			writeChunk(len, true);
		}
		finally {
			// Release the pooled buffer exactly once, even if writeChunk fails.
			ByteBufferPool.release(utf8Buffer);
		}
	}

	private void writeChunk(int len, boolean isLast) {
		try {
			final var buf = buffer;
			final var ubuf = utf8Buffer;
			if (hi == -1 && len <= ubuf.length && len <= buf.length) {
				int i = 0;
				if (LITTLE_ENDIAN && len >= 8) {
					final var src = bufSeg;
					final var dst = utf8Seg;
					int limit = len & ~7;
					for (; i < limit; i += 8) {
						long byteOffset = (long) i << 1;
						long w0 = src.get(LONG_UNALIGNED, byteOffset);
						long w1 = src.get(LONG_UNALIGNED, byteOffset + 8);
						if (((w0 | w1) & NON_ASCII_MASK) != 0) {
							break;
						}
						// Narrow 8 little-endian UTF-16 chars (w0, w1) into 8 packed ASCII bytes
						long packed0 = w0 & 0x00FF_00FF_00FF_00FFL;
						packed0 = (packed0 | (packed0 >>> 8)) & 0x0000_FFFF_0000_FFFFL;
						packed0 = (packed0 | (packed0 >>> 16)) & 0x0000_0000_FFFF_FFFFL;

						long packed1 = w1 & 0x00FF_00FF_00FF_00FFL;
						packed1 = (packed1 | (packed1 >>> 8)) & 0x0000_FFFF_0000_FFFFL;
						packed1 = (packed1 | (packed1 >>> 16)) & 0x0000_0000_FFFF_FFFFL;

						dst.set(LONG_UNALIGNED, i, packed0 | (packed1 << 32));
					}
				}
				// Scalar tail (remaining 0..7 chars) or exact non-ASCII stop point
				for (; i < len; i++) {
					char c = buf[i];
					if (c > 127) {
						break;
					}
					ubuf[i] = (byte) c;
				}
				if (i == len) {
					if (len > 0) {
						out.write(ubuf, 0, len);
					}
					return;
				}
				drainSlow(buf, ubuf, i, i, len, isLast);
				return;
			}
			drainSlow(buf, ubuf, 0, 0, len, isLast);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void drainSlow(char[] buf, byte[] ubuf, int cp, int up, int len, boolean isLast) throws IOException {
		final var o = out;
		if (hi != -1) {
			if (len == 0) {
				if (isLast) {
					up = writeThreeBytes(ubuf, 0, hi);
					hi = -1;
					o.write(ubuf, 0, up);
				}
				return;
			}
			int packed = resolveDeferredHigh(buf, ubuf);
			up = packed >>> 16;
			cp = packed & 0xffff;
		}

		while (true) {
			int max = BUF_LEN - up;
			int rem = len - cp;
			if (rem < max) {
				max = rem;
			}
			int limit = cp + max;
			while (cp < limit) {
				char c = buf[cp];
				if (c > 127) {
					break;
				}
				ubuf[up++] = (byte) c;
				cp++;
			}
			if (cp == len) {
				if (up > 0) {
					o.write(ubuf, 0, up);
				}
				break;
			}
			if (up > BUF_LEN - 4) {
				o.write(ubuf, 0, up);
				up = 0;
			}
			if (cp == limit) {
				continue;
			}

			// Re-read the non-ASCII char that broke the inner loop
			int c = buf[cp++];
			if ((c & 0xfffff800) == 0) { // 2 bytes
				ubuf[up++] = (byte) (0xc0 | (c >> 6));
				ubuf[up++] = (byte) (0x80 | (c & 0x3f));
			} else if (c < 0x8000) { // 3 bytes
				up = writeThreeBytes(ubuf, up, c);
			} else if ((c & 0xfc00) == 0xd800) {
				if (cp < len && (buf[cp] & 0xfc00) == 0xdc00) {
					up = writeFourBytes(ubuf, up, (c << 10) + buf[cp++] + SUPP_OFFSET);
				} else if (cp == len && !isLast) {
					hi = c;
				} else {
					up = writeThreeBytes(ubuf, up, c);
				}
			} else {
				up = writeThreeBytes(ubuf, up, c);
			}
		}
	}

	private int resolveDeferredHigh(char[] buf, byte[] ubuf) {
		int c0 = buf[0];
		int up;
		int cp;
		if ((c0 & 0xfc00) == 0xdc00) {
			up = writeFourBytes(ubuf, 0, (hi << 10) + c0 + SUPP_OFFSET);
			cp = 1;
		} else {
			up = writeThreeBytes(ubuf, 0, hi);
			cp = 0;
		}
		hi = -1;
		return (up << 16) | cp;
	}

	/**
	 * Write one char as 3 UTF-8 bytes; returns the new position.
	 */
	private static int writeThreeBytes(byte[] ubuf, int up, int c) {
		ubuf[up++] = (byte) (0xe0 | (c >> 12));
		ubuf[up++] = (byte) (0x80 | ((c >> 6) & 0x3f));
		ubuf[up++] = (byte) (0x80 | (c & 0x3f));
		return up;
	}

	/**
	 * Write one supplementary codepoint as 4 UTF-8 bytes; returns the new position.
	 */
	private static int writeFourBytes(byte[] ubuf, int up, int c) {
		ubuf[up++] = (byte) (0xf0 | (c >> 18));
		ubuf[up++] = (byte) (0x80 | ((c >> 12) & 0x3f));
		ubuf[up++] = (byte) (0x80 | ((c >> 6) & 0x3f));
		ubuf[up++] = (byte) (0x80 | (c & 0x3f));
		return up;
	}

}
