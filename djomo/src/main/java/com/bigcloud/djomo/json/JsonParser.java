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
package com.bigcloud.djomo.json;

import java.io.IOException;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.base.BaseParser;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.error.UnexpectedPrimitiveException;
import com.bigcloud.djomo.internal.CharSequenceParser;
import com.bigcloud.djomo.internal.FloatingParser;
import com.bigcloud.djomo.io.Buffer;

public class JsonParser extends BaseParser implements Parser {
	final Buffer input;
	final Buffer overflow;

	public JsonParser(Models context, Buffer input, Buffer overflow, ParserFilterFactory... filters) {
		super(context, filters);
		this.input = input;
		this.overflow = overflow;
	}

	@Override
	public Object parse(Model definition) {
		try {
			final var input = this.input;
			final var buf = input.buffer;
			int rp = input.readPosition;
			int wp = input.writePosition;
			while (true) {
				if (rp == wp) {
					if (!input.refill()) {
						throw new ModelException("Model incomplete at " + input.describe());
					}
					rp = 0;
					wp = input.writePosition;
				}
				switch (buf[rp]) {
				case '{':
					input.readPosition = rp;
					return parseObjectModel(definition);
				case '[':
					input.readPosition = rp;
					return parseListModel(definition);
				case '"':
					input.readPosition = rp;
					return parseStringModel(definition);
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					rp++;
					break;
				case 't':
				case 'f':
					input.readPosition = rp;
					return parseBooleanModel(definition);
				case 'n':
					input.readPosition = rp;
					return parseNullModel(definition);
				default:
					input.readPosition = rp;
					return parseNumberModel(definition);
				}
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing JSON", e);
		}
	}

	@Override
	public Object parseObject(
			ObjectModel model) {
		final Object maker = objectMaker(model);
		final var input = this.input;
		final var buf = input.buffer;
		final var t = this.parser;
		final var o = this.overflow;
		try {
			int rp = input.readPosition;
			int wp = input.writePosition;
			boolean first = true;
			while (true) {
				if (rp == wp) {
					if (!input.refill()) {
						throw new ModelException("Model incomplete at " + input.describe());
					}
					rp = 0;
					wp = input.writePosition;
				}
				switch (buf[rp]) {
				case '"':
					input.readPosition = rp;
					Field f = t.parseObjectField(model, CharSequenceParser.parse(input, o));
					if(f != null) {
						f.parse(maker, t);
					}
					else {
						parser.parse(models.anyModel);
					}
					rp = input.readPosition;
					wp = input.writePosition;
					break;
				case '}':
					input.readPosition = rp + 1;
					return model.make(maker);
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
					++rp;
					break;
				case '{':
					if(first) {
						++rp;
						break;
					}
				default:
					throw new ModelException("Unexpected character "+String.valueOf((char)buf[rp])+" at position "+rp+" in " + input.describe());
				}
				first = false;
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing object fields", e);
		}
	}

	@Override
	public Field parseObjectField(
			ObjectModel model, CharSequence field) {
		try {
			Field mfield = model.getField(field);
			final var buf = this.input;
			while (true) {
				switch (buf.read()) {
				case -1:
					throw new ModelException("Model incomplete at " + buf.describe());
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					break;
				case ':':
					return mfield;
				default:
					throw new ModelException("Unexpected character at " + buf.describe());
				}
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing object fields", e);
		}
	}

	@Override
	public  Object parseList(ListModel definition) {
		Object maker = listMaker(definition);
		final var input = this.input;
		final var buf = input.buffer;
		final var t = this.parser;
		try {
			int rp = input.readPosition;
			int wp = input.writePosition;
			boolean first = true;
			LOOP: while (true) {
				if (rp == wp) {
					if (!input.refill()) {
						throw new ModelException("Model incomplete at " + input.describe());
					}
					rp = 0;
					wp = input.writePosition;
				}
				switch (buf[rp]) {
				case ']':
					input.readPosition = rp + 1;
					break LOOP;
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
					++rp;
					break;
				case '[':
					if(first) {
						++rp;
						break;
					}
				default:
					input.readPosition = rp;
					definition.parseItem(maker, t);
					rp = input.readPosition;
					wp = input.writePosition;
				}
				first = false;
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing list items", e);
		}
		return definition.make(maker);
	}

	@Override
	public Object parseNull() {
		var b = this.input;
		int c;
		try {
			if ((c = b.read()) != 'n' || (c = b.read()) != 'u' || (c = b.read()) != 'l' || (c = b.read()) != 'l') {
				throw new UnexpectedPrimitiveException("Unexepected character in null " + (char) c);
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing null", e);
		}
		return null;
	}

	@Override
	public int parseInt() {
		// CPD-OFF
		try {
			final var buf = input.buffer;
			int rp = input.readPosition;
			int wp = input.writePosition;
			int ip = rp;
			boolean negative = false;
			int value = 0;
			// first loop / happy path
			for (; rp < wp; rp++) {
				int ch = buf[rp];
				switch (ch) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					value = value * 10 + (ch - 48);
					break;
				case '-':
					if (rp == ip) {
						negative = true;
					} else {
						throw new ModelException("Number format error at " + input.describe());
					}
					break;
				default:
					if (rp == ip || rp == ip + 1 && negative) {
						throw new ModelException("Number format error at " + input.describe());
					}
					input.readPosition = rp;
					return negative ? 0 - value : value;
				}
			}
			// buffer reload needed
			ip = rp - ip;
			rp = 0;
			if (input.refill()) {
				wp = input.writePosition;
				// second loop; any valid number would only span a single buffer boundary
				PARSE_LOOP:
				for (; rp < wp; rp++) {
					int ch = buf[rp];
					switch (ch) {
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						value = value * 10 + (ch - 48);
						break;
					case '-':
						if (rp == 0 && ip == 0) {
							negative = true;
						} else {
							throw new ModelException("Number format error at " + input.describe());
						}
						break;
					default:
						if (ip == 0 && (rp == 0 || rp == 1 && negative)) {
							throw new ModelException("Number format error at " + input.describe());
						}
						break PARSE_LOOP;
					}
				}
				input.readPosition = rp;
			}
			return negative ? 0 - value : value;
		} catch (IOException e) {
			throw new ModelException("Error parsing int", e);
		}
		// CPD-ON
	}

	@Override
	public long parseLong() {
		try {
			final var buf = input.buffer;
			int rp = input.readPosition;
			int wp = input.writePosition;
			int ip = rp;
			boolean negative = false;
			long value = 0;
			// first loop / happy path
			for (; rp < wp; rp++) {
				int ch = buf[rp];
				switch (ch) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					value = value * 10 + (ch - 48);
					break;
				case '-':
					if (rp == ip) {
						negative = true;
					} else {
						throw new ModelException("Number format error at " + input.describe());
					}
					break;
				default:
					if (rp == ip || rp == ip + 1 && negative) {
						throw new ModelException("Number format error at " + input.describe());
					}
					input.readPosition = rp;
					return negative ? 0 - value : value;
				}
			}
			// buffer reload needed
			ip = rp - ip;
			rp = 0;
			if (input.refill()) {
				wp = input.writePosition;
				// second loop; any valid number would only span a single buffer boundary
				PARSE_LOOP:
				for (; rp < wp; rp++) {
					int ch = buf[rp];
					switch (ch) {
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						value = value * 10 + (ch - 48);
						break;
					case '-':
						if (rp == 0 && ip == 0) {
							negative = true;
						} else {
							throw new ModelException("Number format error at " + input.describe());
						}
						break;
					default:
						if (ip == 0 && (rp == 0 || rp == 1 && negative)) {
							throw new ModelException("Number format error at " + input.describe());
						}
						break PARSE_LOOP;
					}
				}
				input.readPosition = rp;
			}
			return negative ? 0 - value : value;
		} catch (IOException e) {
			throw new ModelException("Error parsing long", e);
		}
	}

	@Override
	public float parseFloat() {
		return (float) parseDouble();
	}

	@Override
	public double parseDouble() {
		try {
			final var buf = input.buffer;
			int rp = input.readPosition;
			int wp = input.writePosition;
			if(rp==wp) {
				if (!input.refill()) {
					throw new NumberFormatException("Number Model incomplete at " + input.describe());
				}
				rp = 0;
				wp = input.writePosition;
			}
			int ip = rp;
			// first loop / happy path
			for (; rp < wp; rp++) {
				//System.out.println("Parse loop 1 "+rp+" "+String.valueOf((char) buf[rp]));
				switch (buf[rp]) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case '+':
				case '-':
				case '.':
				case 'N':
				case 'I':
				case 'E':
				case 'e':
				case 'a':
				case 'n':
				case 'f':
				case 'i':
				case 't':
				case 'y':
					break;
				default:
					input.readPosition = rp;
					return FloatingParser.parseNumber(buf, ip, rp-ip);
				}
			}
			System.arraycopy(buf, ip, overflow.buffer, 0, rp - ip);
			// buffer reload needed
			ip = rp - ip;
			rp = 0;
			if (input.refill()) {
				wp = input.writePosition;
				// second loop; any valid number would only span a single buffer boundary
				PARSE_LOOP:
				for (; rp < wp; rp++) {
					//System.out.println("Parse loop 2 "+rp+" "+String.valueOf((char) buf[rp]));
					switch (buf[rp]) {
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
					case '+':
					case '-':
					case '.':
					case 'N':
					case 'I':
					case 'E':
					case 'e':
					case 'a':
					case 'n':
					case 'f':
					case 'i':
					case 't':
					case 'y':
						break;
					default:
						break PARSE_LOOP;
					}
				}
				if(rp > 0) {
					input.readPosition = rp;
					System.arraycopy(buf, 0, overflow.buffer, ip, rp);
				}
			}
			return FloatingParser.parseNumber(overflow.buffer, 0, rp+ip);
		} catch (IOException e) {
			throw new ModelException("Error parsing number", e);
		}
	}

	@Override
	public boolean parseBoolean() {
		try {
			final var buf = input.buffer;
			int rp = input.readPosition;
			int wp = input.writePosition;
			int pos = 0;
			boolean expectTrue = false;
			// CPD-OFF
			// first loop / happy path
			for (; rp < wp; rp++) {
				int ch = buf[rp];
				int p = pos++;
				if (p == 0) {
					if (ch == 't') {
						expectTrue = true;
						continue;
					}
					if (ch == 'f') {
						continue;
					}
				} else if (p == 1) {
					if (expectTrue ? ch == 'r' : ch == 'a') {
						continue;
					}
				} else if (p == 2) {
					if (expectTrue ? ch == 'u' : ch == 'l') {
						continue;
					}
				} else if (p == 3) {
					if (expectTrue) {
						if (ch == 'e') {
							input.readPosition = rp + 1;
							return true;
						}
					} else if (ch == 's') {
						continue;
					}
				} else if (p == 4 && !expectTrue && ch == 'e') {
					input.readPosition = rp + 1;
					return false;
				}
				throw new UnexpectedPrimitiveException("Unexpected input for boolean " + input.describe());
			}
			// CPD-ON
			// buffer reload needed
			if (!input.refill()) {
				throw new UnexpectedPrimitiveException("Model incomplete at " + input.describe());
			}
			rp = 0;
			wp = input.writePosition;
			// second loop; any valid number would only span a single buffer boundary
			for (; rp < wp; rp++) {
				int ch = buf[rp];
				int p = pos++;
				if (p == 0) {
					if (ch == 't') {
						expectTrue = true;
						continue;
					}
					if (ch == 'f') {
						continue;
					}
				} else if (p == 1) {
					if (expectTrue ? ch == 'r' : ch == 'a') {
						continue;
					}
				} else if (p == 2) {
					if (expectTrue ? ch == 'u' : ch == 'l') {
						continue;
					}
				} else if (p == 3) {
					if (expectTrue) {
						if (ch == 'e') {
							input.readPosition = rp + 1;
							return true;
						}
					} else if (ch == 's') {
						continue;
					}
				} else if (p == 4 && !expectTrue && ch == 'e') {
					input.readPosition = rp + 1;
					return false;
				}
				throw new UnexpectedPrimitiveException("Unexpected input for boolean " + input.describe());
			}
			throw new UnexpectedPrimitiveException("Boolean incomplete at " + input.describe());
		} catch (IOException e) {
			throw new ModelException("Error parsing boolean", e);
		}
	}

	@Override
	public CharSequence parseString() {
		try {
			return CharSequenceParser.parse(input, overflow);
		} catch (IOException e) {
			throw new ModelException("Error parsing string", e);
		}
	}

}