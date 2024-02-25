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
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.internal.CharArraySequence;

public class Buffer extends Writer {
	private final Reader source;
	public final CharArraySequence charArraySequence;
	public char[] buffer;
	public int readPosition;
	public int writePosition;

	public Buffer(char[] firstBuffer) {
		this(firstBuffer, null);
	}

	public Buffer(char[] firstBuffer, Reader source) {
		buffer = firstBuffer;
		this.source = source;
		this.charArraySequence = new CharArraySequence(this);
	}

	private char[] reserve(final int target) {
		writePosition = target;
		final var buf = buffer;
		if (buf.length > target) {
			return buf;
		}
		if (target > 268435456) {
			throw new ModelException("Target string length " + target + " is too large, the input is suspicious");
		}
		int nbl = buf.length * 2;
		while (nbl < target) {
			nbl *= 2;
		}
		return buffer = Arrays.copyOf(buf, nbl);
	}

	@Override
	public void write(char[] chars, int start, int length) {
		final int wp = writePosition;
		System.arraycopy(chars, start, reserve(wp + length), wp, length);
	}

	@Override
	public void write(String str) {
		final int wp = writePosition, length = str.length();
		str.getChars(0, length, reserve(wp + length), wp);
	}

	@Override
	public void write(String str, int offset, int length) {
		final int wp = writePosition;
		str.getChars(offset, length + offset, reserve(wp + length), wp);
	}

	@Override
	public void write(int c) {
		final int wp = writePosition;
		reserve(wp + 1)[wp] = (char) c;
	}

	public boolean refill() throws IOException {
		if ((writePosition = source.read(buffer)) == -1) {
			return false;
		}
		readPosition = 0;
		return true;
	}

	public int read() throws IOException {
		int rp = readPosition;
		if (rp == writePosition) {
			if ((writePosition = source.read(buffer)) == -1) {
				return -1;
			}
			rp = 0;
		}
		readPosition = rp + 1;
		return buffer[rp];
	}

	public void unread() {
		--readPosition;
	}

	public String toString() {
		var rp = readPosition;
		return String.valueOf(buffer, rp, writePosition - rp);
	}

	public String toString(int offset, int length) {
		return String.valueOf(buffer, offset, length);
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}

	public String describe() {
		int start = readPosition - 10;
		if (start < 0) {
			start = 0;
		}
		int end = start + 20;
		if (end > writePosition) {
			end = writePosition;
		}
		if (end < start) {
			end = start;
		}
		return String.valueOf(buffer, start, end - start);
	}
}
