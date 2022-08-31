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
package com.bigcloud.djomo.json;

import java.util.Arrays;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.base.BaseVisitor;
import com.bigcloud.djomo.filter.FilterVisitor;
import com.bigcloud.djomo.io.CharSink;

public class BaseJsonWriter extends BaseVisitor implements Printer, AutoCloseable{
	private static final char[] NULL = { 'n', 'u', 'l', 'l' };
	protected static final int BUF_LEN = 4096;
	private static final ThreadLocal<char[]> localBuffer = new ThreadLocal<>() {
		public char[] initialValue() {
			return new char[BUF_LEN];
		}
	};
	private static final ThreadLocal<char[]> localStrBuffer = new ThreadLocal<>() {
		public char[] initialValue() {
			return new char[BUF_LEN];
		}
	};
	private static final char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static final boolean[] special = new boolean[65536];
	static {
		Arrays.fill(special, 0, 32, true);
		special['"'] = true;
		special['\\'] = true;
		special['\u2028'] = true;
		special['\u2029'] = true;
	}
	protected final char[] buffer = localBuffer.get();
	protected final char[] strBuffer = localStrBuffer.get();
	protected final CharSink sink;
	int pos = 0;
	boolean first;

	public BaseJsonWriter(Models context, CharSink sink, FilterVisitor... filters) {
		super(context, filters);
		this.sink = sink;
		sink.buffer(buffer);
	}

	protected final void reserve(int len) {
		if (BUF_LEN - pos < len) {
			sink.next(pos);
			pos = 0;
		}
	}
	
	@Override
	public void visitNull() {
		raw(NULL, 0, 4);
	}
	
	@Override
	public <T> void visitSimple(T model, SimpleModel<T> definition) {
		definition.print(model, this);
	}

	@Override
	public void quote(String str) {
		var sbuf = strBuffer;
		var buf = buffer;
		var spec = special;
		var lpos = pos;
		if (lpos == BUF_LEN) {
			sink.next(BUF_LEN);
			lpos = 0;
		}
		buf[lpos++] = '"';
		int len = str.length();
		int start = 0;
		int room = BUF_LEN - lpos;
		if (len < room) {
			room = len;
		}
		int c = 0, p, l, i;
		while (true) {
			str.getChars(start, start + room, buf, lpos);
			for (i = 0; i < room; i++) {
				c = buf[lpos];
				if (spec[c]) {
					break;
				}
				++lpos;
			}
			if (i < room) {
				p = 0;
				int rem = room - i - 1;
				System.arraycopy(buf, lpos + 1, sbuf, 0, rem);
				while (true) {
					if (BUF_LEN - lpos < 2) {
						sink.next(lpos);
						lpos = 0;
					}
					buf[lpos++] = '\\';
					switch (c) {
					case '\n':
						buf[lpos++] = 'n';
						break;
					case '\r':
						buf[lpos++] = 'r';
						break;
					case '\t':
						buf[lpos++] = 't';
						break;
					case '\f':
						buf[lpos++] = 'f';
						break;
					case '\b':
						buf[lpos++] = 'b';
						break;
					case '"':
						buf[lpos++] = '"';
						break;
					case '\\':
						buf[lpos++] = '\\';
						break;
					default:
						buf[lpos++] = 'u';
						if (BUF_LEN - lpos < 4) {
							sink.next(lpos);
							lpos = 0;
						}
						final char[] hex = hexchars;
						buf[lpos++] = hex[c / 4096];
						c = c % 4096;
						buf[lpos++] = hex[c / 256];
						c = c % 256;
						buf[lpos++] = hex[c / 16];
						c = c % 16;
						buf[lpos++] = hex[c];
					}
					for (i = p; i < rem; i++) {
						c = sbuf[i];
						if (spec[c]) {
							break;
						}
					}
					if (i > p) {
						l = i - p;
						if (BUF_LEN - lpos < l) {
							sink.next(lpos);
							lpos = 0;
						}
						System.arraycopy(sbuf, p, buf, lpos, l);
						lpos += l;
					}
					if (i == rem) {
						break;
					}
					p = i + 1;
				}
			}
			if ((len -= room) == 0) {
				break;
			}
			start += room;
			if (lpos == BUF_LEN) {
				sink.next(lpos);
				lpos = 0;
			}
			room = BUF_LEN - lpos;
			if (len < room) {
				room = len;
			}
		}
		if (lpos == BUF_LEN) {
			sink.next(BUF_LEN);
			lpos = 0;
		}
		buf[lpos] = '"';
		pos = lpos + 1;
	}

	@Override
	public void raw(char[] chars, int offset, int len) {
		var p = pos;
		int room = BUF_LEN - p;
		while (room < len) {
			System.arraycopy(chars, offset, buffer, p, room);
			sink.next(BUF_LEN);
			offset += room;
			p = 0;
			len -= room;
			room = BUF_LEN;
		}
		System.arraycopy(chars, offset, buffer, p, len);
		pos = p + len;
	}

	@Override
	public void raw(String str) {
		int len = str.length();
		var p = pos;
		int room = BUF_LEN - p;
		int offset = 0;
		while (room < len) {
			str.getChars(offset, offset + room, buffer, p);
			sink.next(BUF_LEN);
			offset += room;
			p = 0;
			len -= room;
			room = BUF_LEN;
		}
		str.getChars(offset, offset + len, buffer, p);
		pos = p + len;
	}

	@Override
	public void close() {
		sink.last(pos);
	}
}
