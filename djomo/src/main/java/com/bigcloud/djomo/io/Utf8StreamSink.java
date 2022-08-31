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

public class Utf8StreamSink implements CharSink {
	private static final int BUF_LEN = 4096;
	private static final ThreadLocal<byte[]> localUtf8Buffer = new ThreadLocal<>() {
		public byte[] initialValue() {
			return new byte[BUF_LEN];
		}
	};
	private final byte[] utf8Buffer = localUtf8Buffer.get();
	private final OutputStream out;
	char[] buffer;
	int hi = -1;

	public Utf8StreamSink(OutputStream out) {
		this.out = out;
	}

	@Override
	public void buffer(char[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public void next(int len) {

		try {
			final var buf = buffer;
			final var ubuf = utf8Buffer;
			final var o = out;
			int up = 0;
			int cp = 0;
			while (true) {
				int max = BUF_LEN - up;
				int rem = len - cp;
				if (rem < max) {
					max = rem;
				}
				int c = 0;
				int i;
				for (i=0; i < max; i++) {
					c = buf[cp++];
					if(c > 127) {
						break;
					}
					ubuf[up++] = (byte) c;
				}
				if (cp == len && i == max) {
					// made it
					if (up > 0) {
						o.write(ubuf, 0, up);
					}
					break;
				}
				if (up > BUF_LEN - 4) {
					o.write(ubuf, 0, up);
					up = 0;
				}
				if (i==max) {
					continue;
				}
				// at this point we hit a multi-byte char
				if ((c & 0xfffff800) == 0) { // 2 bytes.
					ubuf[up++] = (byte) (0xc0 | (c >> 6));
					ubuf[up++] = (byte) (0x80 | (c & 0x3f));
				} else if ((c & 0xfffd8000) == 0) { // 3 bytes.
					ubuf[up++] = (byte) (0xe0 | (c >> 12));
					ubuf[up++] = (byte) (0x80 | ((c >> 6) & 0x3f));
					ubuf[up++] = (byte) (0x80 | (c & 0x3f));
				} else {
					if (cp == len) {
						hi = c;
					} else {
						if(hi != -1) {
							c = (hi << 10) + c + 0x10000 - (0xD800 << 10) - 0xDC00;
							hi = -1;
						}
						else {
							c = (c << 10) + buf[cp++] + 0x10000 - (0xD800 << 10) - 0xDC00;
						}
						ubuf[up++] = (byte) (0xf0 | (c >> 18));
						ubuf[up++] = (byte) (0x80 | ((c >> 12) & 0x3f));
						ubuf[up++] = (byte) (0x80 | ((c >> 6) & 0x3f));
						ubuf[up++] = (byte) (0x80 | (c & 0x3f));
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void last(int len) {
		next(len);
	}

}
