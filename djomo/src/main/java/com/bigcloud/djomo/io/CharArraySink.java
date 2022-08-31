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

import java.util.Arrays;

public class CharArraySink implements CharSink {
	private char[] sunk;
	private char[] buffer;
	private int last = -1;

	@Override
	public void buffer(char[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public void next(int len) {
		if (sunk == null) {
			sunk = Arrays.copyOf(buffer, len);
		} else {
			int sl = sunk.length;
			sunk = Arrays.copyOf(sunk, sl + len);
			System.arraycopy(buffer, 0, sunk, sl, len);
		}
	}

	@Override
	public void last(int len) {
		if (sunk == null) {
			last = len;
		} else {
			int sl = sunk.length;
			sunk = Arrays.copyOf(sunk, sl + len);
			System.arraycopy(buffer, 0, sunk, sl, len);
		}
	}

	public String toString() {
		if (last > 0) {
			return new String(buffer, 0, last);
		}
		return new String(sunk);
	}
}
