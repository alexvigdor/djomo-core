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

	public boolean refill() {
		try {
			writePosition = source.read(buffer);
		} catch (IOException e) {
			throw new ModelException("Error reading input", e);
		}
		if (writePosition == -1) {
			return false;
		}
		readPosition = 0;
		return true;
	}
	
	public void refillStrict() {
		if(!refill()) {
			throw new ModelException("Unexpected EOF");
		}
	}

	public char seek() {
		int rp = readPosition;
		var buf = buffer;
		while(true) {
			int wp = writePosition;
			while(rp < wp) {
				char c = buf[rp];
				switch(c) {
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
					case ',':
						++rp;
						break;
					default:
						readPosition = rp;
						writePosition = wp;
						return c;
				}
			}
			refillStrict();
			rp = 0;
		}
	}
	
	public char seek(char gobble) {
		int rp = readPosition;
		var buf = buffer;
		while(true) {
			int wp = writePosition;
			while(rp < wp) {
				char c = buf[rp];
				switch(c) {
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
					case ',':
						++rp;
						break;
					default:
						if(c == gobble) {
							++rp;
						}
						readPosition = rp;
						writePosition = wp;
						return c;
				}
			}
			refillStrict();
			rp = 0;
		}
	}
	
	public void expect(char target) {
		int rp = readPosition;
		var buf = buffer;
		while(true) {
			int wp = writePosition;
			while(rp < wp) {
				char c = buf[rp++];
				switch(c) {
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
						break;
					default:
						if(c != target) {
							throw new ModelException("Expected "+target+" but found " + c+" at "+ describe());
						}
						readPosition =  rp ;
						writePosition = wp;
						return;
				}
			}
			refillStrict();
			rp = 0;
		}
	}
	
	public void expect(char[] target) {
		int rp = readPosition;
		var buf = buffer;
		int tp = 0;
		while(true) {
			int wp = writePosition;
			while(rp < wp) {
				char c = buf[rp++];
				switch(c) {
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
						if(tp == 0) {
							break;
						}
					default:
						if(c != target[tp++]) {
							throw new ModelException("Expected "+new String(target)+" but found " + describe());
						}
						if(tp == target.length) {
							readPosition = rp;
							writePosition = wp;
							return;
						}
				}
			}
			refillStrict();
			rp = 0;
		}
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
