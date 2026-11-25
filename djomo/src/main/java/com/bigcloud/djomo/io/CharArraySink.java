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

/**
 * A {@link CharSink} that accumulates the entire stream as a char array.
 *
 * <p>
 * Every chunk is copied out of the supplied buffer as it arrives. In particular the final
 * chunk is copied eagerly in {@link #last(int)}: the buffer is owned by the writing
 * {@link com.bigcloud.djomo.json.BaseJsonWriter} and is returned to the shared buffer
 * pool as soon as {@code last()} returns, so it must never be read after that point.
 * </p>
 */
public class CharArraySink implements CharSink {
	private char[] sunk;
	private char[] buffer;

	@Override
	public void buffer(char[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public void next(int len) {
		append(len);
	}

	@Override
	public void last(int len) {
		// Copy eagerly; the buffer is recycled by the writer's buffer pool
		// as soon as this call returns and must not be retained.
		append(len);
	}

	private void append(int len) {
		if (sunk == null) {
			sunk = Arrays.copyOf(buffer, len);
		} else {
			int sl = sunk.length;
			sunk = Arrays.copyOf(sunk, sl + len);
			System.arraycopy(buffer, 0, sunk, sl, len);
		}
	}

	@Override
	public String toString() {
		return sunk == null ? "" : new String(sunk);
	}
}
