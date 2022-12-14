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

import java.io.EOFException;
import java.io.IOException;

import com.bigcloud.djomo.io.Buffer;
/**
 * This class parses and returns a volatile char sequence representing a decoded string;
 * the caller must use the CharSequence before reading more data from the readBuffer
 * 
 * @author Alex Vigdor
 *
 */
public class CharSequenceParser {
	public static CharSequence parse(Buffer readBuffer, Buffer writeBuffer) throws IOException {
		// happy path; first character is already known to be a quote
		int rp = readBuffer.readPosition+1, start = rp;
		int wp = readBuffer.writePosition;
		char[] rb = readBuffer.buffer;
		char r = 0;
		for (; rp < wp; rp++) {
			r = rb[rp];
			if (r == '"' || r == '\\') {
				break;
			}
		}
		if (r == '"') {
			// System.out.println("FOUND HAPPY QUOTED STRING "+readBuffer.toString(start,
			// rp-start));
			readBuffer.readPosition = rp + 1;
			return new CharArraySequence(rb, start, rp - start);
		}
		writeBuffer.writePosition = 0;
		OUTER: while (true) {
			if (rp == wp) {
				writeBuffer.write(rb, start, rp - start);
				if (!readBuffer.refill()) {
					throw new EOFException("Expected closing quote, reached EOF instead");
				}
				rp = start = 0;
				wp = readBuffer.writePosition;
			}
			r = rb[rp++];
			if (r == '"') {
				writeBuffer.write(rb, start, rp - 1 - start);
				readBuffer.readPosition = rp;
				// System.out.println("FOUND SAD QUOTED STRING "+writeBuffer.toString(0,
				// writeBuffer.writePosition));
				return new CharArraySequence(writeBuffer.buffer, 0, writeBuffer.writePosition);
			}
			if (r == '\\') {
				writeBuffer.write(rb, start, rp - 1 - start);
				if (rp == wp) {
					if (!readBuffer.refill()) {
						throw new EOFException("Expected closing quote, reached EOF instead");
					}
					rp = start = 0;
					wp = readBuffer.writePosition;
				}
				r = rb[rp++];
				switch (r) {
					case 'n':
						writeBuffer.write('\n');
						break;
					case 'r':
						writeBuffer.write('\r');
						break;
					case 't':
						writeBuffer.write('\t');
						break;
					case 'f':
						writeBuffer.write('\f');
						break;
					case 'b':
						writeBuffer.write('\b');
						break;
					case 'u':
						int accum = 0;
						boolean ec6 = false;
						int chars = 0;
						ESCAPE: while (true) {
							if (rp == wp) {
								if (!readBuffer.refill()) {
									throw new EOFException("Expected closing quote, reached EOF instead");
								}
								rp = start = 0;
								wp = readBuffer.writePosition;
							}
							r = rb[rp++];
							int p = 0;
							switch (r) {
								case '0':
									break;
								case '1':
									p = 1;
									break;
								case '2':
									p = 2;
									break;
								case '3':
									p = 3;
									break;
								case '4':
									p = 4;
									break;
								case '5':
									p = 5;
									break;
								case '6':
									p = 6;
									break;
								case '7':
									p = 7;
									break;
								case '8':
									p = 8;
									break;
								case '9':
									p = 9;
									break;
								case 'A':
								case 'a':
									p = 10;
									break;
								case 'B':
								case 'b':
									p = 11;
									break;
								case 'C':
								case 'c':
									p = 12;
									break;
								case 'D':
								case 'd':
									p = 13;
									break;
								case 'E':
								case 'e':
									p = 14;
									break;
								case 'F':
								case 'f':
									p = 15;
									break;
								case '}':
									break ESCAPE;
								case '{':
									if (chars == 0) {
										ec6 = true;
										continue;
									}
								default:
									// drop invalid
									start = --rp;
									continue OUTER;
							}
							accum = accum * 16 + p;
							if (!ec6 && ++chars == 4) {
								break;
							}
						}
						writeBuffer.write((char) accum);
						break;
					default:
						writeBuffer.write(r);
				}
				start = rp;
			}
			for (; rp < wp; rp++) {
				r = rb[rp];
				if (r == '"' || r == '\\') {
					break;
				}
			}
		}

	}
}
