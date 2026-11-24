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
import java.io.InputStream;
import java.io.Reader;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * A {@link Reader} over a UTF-8 byte source.
 *
 * <p>Decoding is lenient: malformed bytes never throw and never produce U+FFFD;
 * the reader fabricates the best characters it can from whatever bytes are present
 * (see {@code Utf8StreamReaderMalformedTest} for the pinned behavior).
 *
 * <p>{@link #read(char[], int, int)} requires at least one destination slot, and may
 * return 0 (with len &gt; 0) when the underlying stream window ends mid-sequence and
 * nothing else is available in it; the pending bytes complete on the next read.
 */
public class Utf8StreamReader extends Reader {
	private static final int BUF_LEN = 8192;
	private static final ValueLayout.OfLong LONG_LE =
			ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

	private static final ThreadLocal<byte[]> localUtf8Buffer = ThreadLocal.withInitial(() -> new byte[BUF_LEN]);

	private final byte[] utf8Buffer;
	private final MemorySegment ubufSeg;
	final InputStream stream;

	private char[] lastCbuf;
	private MemorySegment cbufSeg;

	int pointer = 0;
	int limit = 0;
	int leftoverCode;
	int remaining;
	int trail = -1;

	public Utf8StreamReader(InputStream stream) {
		this.stream = stream;
		this.utf8Buffer = localUtf8Buffer.get();
		this.ubufSeg = MemorySegment.ofArray(utf8Buffer);
	}

	public Utf8StreamReader(byte[] data) {
		this.stream = null;
		this.utf8Buffer = data;
		this.ubufSeg = MemorySegment.ofArray(data);
		this.limit = data.length;
	}

	private static long unpackLo(long w) {
		// Spread 4 ascii bytes b0..b3 (the low 32 bits of w, little-endian, b0 at offset 0)
		// into char lanes: [b0, 0, b1, 0, b2, 0, b3, 0]
		long x = w & 0x00000000FFFFFFFFL;
		x = (x | (x << 16)) & 0x0000FFFF0000FFFFL;
		return (x | (x << 8)) & 0x00FF00FF00FF00FFL;
	}

	private static long unpackHi(long w) {
		// Spread 4 ascii bytes b4..b7 (the high 32 bits of w, little-endian, b4 at offset 4)
		// into char lanes: [b4, 0, b5, 0, b6, 0, b7, 0]
		long x = w >>> 32;
		x = (x | (x << 16)) & 0x0000FFFF0000FFFFL;
		return (x | (x << 8)) & 0x00FF00FF00FF00FFL;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		final byte[] ubuf = utf8Buffer;
		final MemorySegment uSeg = ubufSeg;
		final MemorySegment cSeg = (cbuf == lastCbuf) ? cbufSeg : (cbufSeg = MemorySegment.ofArray(lastCbuf = cbuf));

		int p = pointer;
		int lim = limit;
		int wrote = 0;

		if (trail != -1) {
			cbuf[off++] = (char) trail;
			trail = -1;
			wrote++;
		}
		if (p == lim) {
			if (stream == null) {
				return wrote > 0 ? wrote : -1;
			}
			lim = stream.read(ubuf);
			if (lim == -1) {
				return wrote > 0 ? wrote : -1;
			}
			p = 0;
		}

		int code = leftoverCode;
		int rem = remaining;

		// 1. Drain any multi-byte character split across the previous read() boundary
		if (rem > 0) {
			for (; rem > 0 && p < lim; rem--) {
				code = (code << 6) | (ubuf[p++] & 0x3F);
			}
			if (rem == 0) {
				if (code > 0xFFFF) {
					cbuf[off++] = (char) (0xD800 - (0x10000 >> 10) + (code >> 10));
					wrote++;
					if (wrote == len) {
						trail = 0xDC00 + (code & 0x3FF);
						pointer = p;
						limit = lim;
						remaining = 0;
						return wrote;
					}
					cbuf[off++] = (char) (0xDC00 + (code & 0x3FF));
					wrote++;
				} else {
					cbuf[off++] = (char) code;
					wrote++;
				}
			} else {
				leftoverCode = code;
				pointer = p;
				limit = lim;
				remaining = rem;
				return wrote;
			}
		}

		// 2. Main loop: 16-byte SWAR ASCII + inline 2-byte/3-byte UTF-8 fast paths
		while (wrote < len && p < lim) {
			int max = Math.min(len - wrote, lim - p);

			// 2a. 16-byte unrolled SWAR ASCII fast path
			if (max >= 16) {
				long uOff = p;
				long cOff = (long) off << 1;
				long uEnd16 = p + (max & ~15);

				while (uOff < uEnd16) {
					long w0 = uSeg.get(LONG_LE, uOff);
					long w1 = uSeg.get(LONG_LE, uOff + 8L);
					if (((w0 | w1) & 0x8080808080808080L) != 0L) {
						break;
					}
					cSeg.set(LONG_LE, cOff, unpackLo(w0));
					cSeg.set(LONG_LE, cOff + 8L, unpackHi(w0));
					cSeg.set(LONG_LE, cOff + 16L, unpackLo(w1));
					cSeg.set(LONG_LE, cOff + 24L, unpackHi(w1));
					uOff += 16L;
					cOff += 32L;
				}

				int consumed = (int) (uOff - p);
				p += consumed;
				off += consumed;
				wrote += consumed;
				max -= consumed;
			}

			// 2b. Single 8-byte SWAR tail check (catches remaining 8..15 bytes or the clean half of a 16-byte break)
			if (max >= 8) {
				long w = uSeg.get(LONG_LE, p);
				if ((w & 0x8080808080808080L) == 0L) {
					long cOff = (long) off << 1;
					cSeg.set(LONG_LE, cOff, unpackLo(w));
					cSeg.set(LONG_LE, cOff + 8L, unpackHi(w));
					p += 8;
					off += 8;
					wrote += 8;
					max -= 8;
				}
			}

			// 2c. Scalar ASCII & inline multi-byte fast path
			int end = p + max;
			while (p < end) {
				byte c = ubuf[p++];
				if (c >= 0) {
					cbuf[off++] = (char) c;
					wrote++;
					continue;
				}

				// Multi-byte lead byte encountered:
				if ((c & 0xE0) == 0xC0) {
					// 2-byte sequence: decode inline if 1 more byte is available in buffer
					if (p < lim) {
						cbuf[off++] = (char) (((c & 0x1F) << 6) | (ubuf[p++] & 0x3F));
						wrote++;
						// If we consumed beyond `end` (which happens when `len - wrote` was the tight bound),
						// `p < end` will naturally terminate on the next check!
						continue;
					}
					code = c & 0x1F;
					rem = 1;
					break;
				} else if ((c & 0xF0) == 0xE0) {
					// 3-byte sequence: decode inline if 2 more bytes are available in buffer
					if (p + 1 < lim) {
						int b1 = ubuf[p++] & 0x3F;
						int b2 = ubuf[p++] & 0x3F;
						cbuf[off++] = (char) (((c & 0x0F) << 12) | (b1 << 6) | b2);
						wrote++;
						continue;
					}
					code = c & 0x0F;
					rem = 2;
					break;
				} else {
					// 4-byte sequence (or malformed lead byte): handle via split-safe fallback
					code = c & 0x07;
					rem = 3;
					break;
				}
			}

			// If we broke out due to a boundary-split multi-byte sequence, finish draining what's left in `ubuf`
			if (rem > 0) {
				for (; rem > 0 && p < lim; rem--) {
					code = (code << 6) | (ubuf[p++] & 0x3F);
				}
				if (rem == 0) {
					if (code > 0xFFFF) {
						cbuf[off++] = (char) (0xD800 - (0x10000 >> 10) + (code >> 10));
						wrote++;
						if (wrote == len) {
							trail = 0xDC00 + (code & 0x3FF);
						} else {
							cbuf[off++] = (char) (0xDC00 + (code & 0x3FF));
							wrote++;
						}
					} else {
						cbuf[off++] = (char) code;
						wrote++;
					}
				} else {
					leftoverCode = code;
				}
				break;
			}
		}

		pointer = p;
		limit = lim;
		remaining = rem;
		return wrote;
	}

	@Override
	public void close() throws IOException {
	}
}
