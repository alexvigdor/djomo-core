/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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
package com.bigcloud.djomo.simple;

import java.util.UUID;

import com.bigcloud.djomo.json.SafeCharSequence;

public class UUIDPrinter implements SafeCharSequence {
	private static final char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	final UUID uuid;
	public UUIDPrinter(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public int length() {
		return 36;
	}

	@Override
	public int getChars(char[] dest, int pos) {
		long val = uuid.getMostSignificantBits();
		var hc = hexchars;
		for (int i = 0; i < 36; i++) {
			switch (i) {
			case 18:
				val = uuid.getLeastSignificantBits();
			case 8:
			case 13:
			case 23:
				dest[pos + i] = '-';
				break;
			default:
				int b = (int) (val >>> 60);
				dest[pos + i] = hc[b];
				val = val << 4;
			}
		}
		return pos + 36;
	}

	@Override
	public char charAt(int index) {
		//shouldn't be called in normal serialization
		char[] cs = new char[36];
		getChars(cs, 0);
		return cs[index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		//shouldn't be called in normal serialization
		return toString().subSequence(start, end);
	}
	
	public String toString() {
		//shouldn't be called in normal serialization
		char[] cs = new char[36];
		getChars(cs, 0);
		return new String(cs);
	}

}
