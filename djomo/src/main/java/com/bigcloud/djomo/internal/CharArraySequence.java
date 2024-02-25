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
package com.bigcloud.djomo.internal;

import com.bigcloud.djomo.io.Buffer;

/**
 * A simple CharSequence used when parsing to avoid creating extra strings for field lookups
 * 
 * @author Alex Vigdor
 *
 */
public class CharArraySequence implements CharSequence {
	protected final Buffer buffer;
	public int start;
	public int len;

	public CharArraySequence(Buffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public char charAt(int index) {
		return buffer.buffer[start + index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		var sub = new CharArraySequence(buffer);
		sub.start = this.start + start;
		sub.len = end-start;
		return sub;
	}

	public int hashCode() {
		char[] b = buffer.buffer;
		int h = 0;
		int i = start, end = i+len;
		for (; i < end; i++) {
			h = 31 * h + b[i];
		}
		return h;
	}

	public boolean equals(Object o) {
		if (o instanceof CharSequence cs) {
			char[] b = buffer.buffer;
			int len = this.len;
			int start = this.start;
			if (cs.length() == len) {
				for (int i = 0; i < len; i++) {
					if (b[start + i] != cs.charAt(i)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return new String(buffer.buffer, start, len);
	}
}
