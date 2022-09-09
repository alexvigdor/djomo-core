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
		int op = 0;
		int rp = input.readPosition;
		int wp = input.writePosition;
		if (rp == wp) {
			if (!input.refill()) {
				throw new NumberFormatException(input.describe());
			}
			wp = input.writePosition;
			rp = 0;
		}
		boolean negative = false;
		boolean decimal = false;
		boolean exponent = false;
		long value = 0;
		char[] expected = null;
		int ep = 0;
		int start = rp;
		// loop: we may need to refill the buffer mid-parse
		PARSE_LOOP: while (true) {
			for (; rp < wp; rp++) {
				int ch = buffer[rp];
				if (expected != null) {
					if (ep == expected.length) {
						break PARSE_LOOP;
					}
					char ex = expected[ep++];
					if (ch != ex) {
						throw new NumberFormatException(input.describe());
					}
					continue;
				}
				if (ch >= 48 && ch <= 57) {
					if (!decimal) {
						value = value * 10 + (ch - 48);
					}
					continue;
				}
				if (ch == '.') {
					// decimal, we stop our calculation and just buffer for Double.parse
					decimal = true;
					continue;
				}
				if (ch == '-') {
					if (!exponent && (rp != start || op != 0)) {
						throw new NumberFormatException(input.describe());
					}
					negative = true;
					continue;
				}
				if (ch == '+') {
					if (!exponent && (rp != start || op != 0)) {
						throw new NumberFormatException(input.describe());
					}
					continue;
				}
				if (ch == 'e' || ch == 'E') {
					if (!decimal || exponent) {
						throw new NumberFormatException(input.describe());
					}
					exponent = true;
					continue;
				}
				if (ch == 'N') {
					expected = new char[] { 'a', 'N' };
					decimal = true;
					continue;
				}
				if (ch != 'I') {
					// only remaining valid case is Infinity
					break PARSE_LOOP;
				}
				expected = new char[] { 'n', 'f', 'i', 'n', 'i', 't', 'y' };
				decimal = true;
			}
			// if we get here, copy contents to overflow and refill buffer
			int len = rp - start;
			System.arraycopy(buffer, start, out, op, len);
			if (!input.refill()) {
				break PARSE_LOOP;
			}
			op += len;
			if (op > 26) {
				// all supported numbers should fit in 26 characters or less
				throw new NumberFormatException("Number is too long " + op + " " + input.describe());
			}
			start = rp = 0;
			wp = input.writePosition;
		}
		input.readPosition = rp;
		if (decimal) {
			String dstr;
			if (op == 0) {
				dstr = new String(buffer, start, rp - start);
			} else {
				if (rp > 0) {
					System.arraycopy(buffer, 0, out, op, rp);
					op += rp;
				}
				dstr = new String(out, 0, op);
			}
			return convert(Double.parseDouble(dstr));
		}
		if (negative) {
			value = -value;
		}
		if (value < Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
			return convertNumber((int) value);
		}
		return convertNumber(value);
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

	protected N convertNumber(Number n) {
		return (N) n;
	}

}
