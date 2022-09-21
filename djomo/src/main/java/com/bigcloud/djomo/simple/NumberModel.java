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
package com.bigcloud.djomo.simple;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.io.Buffer;

public class NumberModel<N extends Number> extends BaseSimpleModel<N> {
	private static final ThreadLocal<char[]> parseBuffer = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[256];
		}
	};

	public NumberModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public void print(N obj, Printer printer) {
		printer.raw(obj.toString());
	}

	@Override
	public N parse(Buffer input, Buffer overflow) throws IOException {
		// numeric
		char[] buffer = input.buffer;
		char[] out = overflow.buffer;
		int rp = input.readPosition;
		int wp = input.writePosition;
		int start = rp;
		if (rp == wp) {
			if (!input.refill()) {
				throw new NumberFormatException(input.describe());
			}
			wp = input.writePosition;
			rp = 0;
		}
		boolean negative = buffer[rp] == '-';
		if (negative) {
			rp++;
		}
		int op = 0;
		long value = 0;
		boolean integral = true;
		PARSE_LOOP: while (true) {
			int ch;
			for (; rp < wp; rp++) {
				ch = buffer[rp];
				switch (ch) {
					case '0': case '1': case '2': case '3': case '4':
					case '5': case '6': case '7': case '8': case '9':
						if(integral) {
							value = value * 10 + (ch - 48);
						}
						break;
					case '+': case '-':
					case '.':
					case 'N':
					case 'I':
					case 'E': case 'e':
					case 'a':
					case 'n': case 'f': case 'i': case 't': case 'y':
						integral = false;
						break;
					default:
						break PARSE_LOOP;
				}
			}
			// if we get here, copy contents to overflow and refill buffer
			int len = rp - start;
			System.arraycopy(buffer, start, out, op, len);
			op += len;
			if (op > 26) {
				// all supported numbers should fit in 26 characters or less
				throw new NumberFormatException("Number is too long " + op + " " + input.describe());
			}
			rp = 0;
			if (!input.refill()) {
				break PARSE_LOOP;
			}
			start = 0;
			wp = input.writePosition;
		}
		input.readPosition = rp;
		if(integral) {
			if (negative) {
				value = -value;
			}
			if (value < Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
				return convertInt((int) value);
			}
			return convertLong(value);
		}
		String dstr;
		if (op == 0) {
			dstr = new String(buffer, start, rp - start);
		} else {
			if (rp > 0) {
				System.arraycopy(buffer, start, out, op, rp);
				op += rp;
			}
			dstr = new String(out, 0, op);
		}
		return convertDouble(Double.parseDouble(dstr));
	}

	@Override
	public N convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (N) o;
		}
		if (o instanceof Number) {
			return convertNumber((Number) o);
		}
		return parse(getParseable(o));
	}

	public N parse(String str) {
		try (StringReader sr = new StringReader(str);) {
			return parse(new Buffer(parseBuffer.get(), sr), new Buffer(new char[26]));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected N convertNumber(Number value) {
		return (N) value;
	}

	protected N convertDouble(double value) {
		return (N) Double.valueOf(value);
	}

	protected N convertInt(int value) {
		return (N) Integer.valueOf(value);
	}

	protected N convertLong(long value) {
		return (N) Long.valueOf(value);
	}

}
