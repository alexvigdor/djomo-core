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
		int rp = input.readPosition;
		int wp = input.writePosition;
		if (rp == wp) {
			if (!input.refill()) {
				throw new NumberFormatException(input.describe());
			}
			wp = input.writePosition;
			rp = 0;
		}
		int ch = buffer[rp++];
		boolean negative = false;
		if (ch == 45) {
			negative = true;
			if (rp == wp) {
				if (!input.refill()) {
					throw new NumberFormatException(input.describe());
				}
				wp = input.writePosition;
				rp = 0;
			}
			ch = buffer[rp++];
		}
		long val = 0;
		while (ch >= 48 && ch <= 57) {
			val = 10 * val + (ch - 48);
			if (rp == wp) {
				if (!input.refill()) {
					ch = 0;
					wp = -1;
					break;
				}
				wp = input.writePosition;
				rp = 0;
			}
			ch = buffer[rp++];
		}
		if (ch == 46) {
			// floating
			double dval = 0, mult = 0.1;
			if (rp == wp) {
				if (!input.refill()) {
					rp = -1;
					wp = -1;
				}
				else {
					wp = input.writePosition;
					rp = 0;
				}
			}
			if(rp >= 0) {
				ch = buffer[rp++];
				while (ch >= 48 && ch <= 57) {
					dval += (ch - 48) * mult;
					mult /= 10;
					if (rp == wp) {
						if (!input.refill()) {
							ch = 0;
							wp = -1;
							break;
						}
						wp = input.writePosition;
						rp = 0;
					}
					ch = buffer[rp++];
				}
			}
			dval += val;
			if (negative) {
				dval = -dval;
			}
			// look for exponential notation here
			if (ch == 'e' || ch == 'E') {
				int exponent = 0;
				boolean inverse = false;
				if (rp == wp) {
					if (!input.refill()) {
						return convertNumber(dval);
					}
					wp = input.writePosition;
					rp = 0;
				}
				ch = buffer[rp++];
				if (ch == '-' || ch == '+') {
					if (ch == '-') {
						inverse = true;
					}
					if (rp == wp) {
						if (!input.refill()) {
							ch = 0;
							wp = -1;
						} else {
							wp = input.writePosition;
							ch = buffer[0];
							rp = 1;
						}
					} else {
						ch = buffer[rp++];
					}
				}
				while (ch >= 48 && ch <= 57) {
					exponent = 10 * exponent + (ch - 48);
					if (rp == wp) {
						if (!input.refill()) {
							ch = 0;
							wp = -1;
							break;
						}
						wp = input.writePosition;
						rp = 0;
					}
					ch = buffer[rp++];
				}
				if (inverse) {
					exponent = -exponent;
				}
				dval = dval * Math.pow(10, exponent);
			}
			input.readPosition = rp - 1;
			input.writePosition = wp;
			return convertNumber(dval);
		}
		input.readPosition = rp - 1;
		input.writePosition = wp;
		if (negative) {
			val = -val;
		}
		if (val < Integer.MAX_VALUE && val > Integer.MIN_VALUE) {
			return convertNumber((int) val);
		}
		return convertNumber(val);
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
			return parse(new Buffer(parseBuffer.get(), sr), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected N convertNumber(Number n) {
		return (N) n;
	}

}
