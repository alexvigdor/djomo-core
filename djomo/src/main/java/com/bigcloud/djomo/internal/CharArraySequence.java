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
/**
 * A simple CharSequence used when parsing to avoid creating extra strings for field lookups
 * 
 * @author Alex Vigdor
 *
 */
public class CharArraySequence implements CharSequence {
	private final char[] buffer;
	private final int start;
	private final int len;

	public CharArraySequence(char[] buffer, int start, int len) {
		this.buffer = buffer;
		this.start = start;
		this.len = len;
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public char charAt(int index) {
		return buffer[start + index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new CharArraySequence(buffer, this.start + start, end - start);
	}

	public int hashCode() {
		char[] b = buffer;
		int h = 0;
		int i = start, end = i+len;
		for (; i < end; i++) {
			h = 31 * h + b[i];
		}
		return h;
	}

	public boolean equals(Object o) {
		if (o instanceof CharSequence cs) {
			char[] b = buffer;
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
		return new String(buffer, start, len);
	}
}
