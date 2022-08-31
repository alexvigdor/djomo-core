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

public class Utf8StreamReader extends Reader {
	private static final int BUF_LEN = 4096;
	private static final ThreadLocal<byte[]> localUtf8Buffer = new ThreadLocal<>() {
		public byte[] initialValue() {
			return new byte[BUF_LEN];
		}
	};
	private final byte[] utf8Buffer;
	final InputStream stream;
	int pointer = 0;
	int limit = 0;
	int leftoverCode;
	int remaining;
	int trail = -1;

	public Utf8StreamReader(InputStream stream) {
		this.stream = stream;
		utf8Buffer = localUtf8Buffer.get();
	}

	public Utf8StreamReader(byte[] data) {
		this.stream = null;
		utf8Buffer = data;
		limit = data.length;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		var ubuf = utf8Buffer;
		int p = pointer;
		int lim = limit;
		if (p == lim) {
			if(stream == null) {
				return -1;
			}
			lim = stream.read(ubuf);
			if(lim == -1) {
				return -1;
			}
			p = 0;
		}
		int cp = off;
		byte c = 0;
		int wrote = 0;
		int code = leftoverCode;
		int rem = remaining;
		if (trail != -1) {
			cbuf[cp++] = (char) trail;
			trail = -1;
		}
		while (true) {
			if (rem > 0) {
				for (; rem > 0 && p < lim; rem--) {
					code = (code << 6) | (ubuf[p++] & 0x3F);
				}
				if (rem == 0) {
					if ((code & 0xfffd8000) != 0) {
						cbuf[cp++] = (char) ((0xD800 - (0x10000 >> 10)) + (code >> 10));
						++wrote;
						if (wrote == len) {
							trail = 0xDC00 + (code & 0x3FF);
						} else {
							cbuf[cp++] = (char) (0xDC00 + (code & 0x3FF));
							++wrote;
						}
					} else {
						cbuf[cp++] = (char) code;
						++wrote;
					}
				} else {
					leftoverCode = code;
				}
			}
			int max = len - wrote;
			int left = lim - p;
			if (max > left) {
				max = left;
			}
			int i = 0;
			for (; i < max; i++) {
				c = ubuf[p++];
				if (c < 0) {
					break;
				}
				cbuf[cp++] = (char) c;
			}
			wrote += i;
			if (i == max) {
				break;
			}
			if ((c & 0xe0) == 0xc0) {
				// 2 byte
				code = c & 0x1F;
				rem = 1;
			} else if ((c & 0xf0) == 0xe0) {
				code = c & 0x0F;
				rem = 2;
			} else {
				code = c & 0x07;
				rem = 3;
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
