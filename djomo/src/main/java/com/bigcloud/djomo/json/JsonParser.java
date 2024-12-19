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

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.base.BaseParser;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.internal.CharSequenceParser;
import com.bigcloud.djomo.internal.FloatingParser;
import com.bigcloud.djomo.io.Buffer;

public class JsonParser extends BaseParser implements Parser {
	final static char[] TRUE_CHARS = { 't', 'r', 'u', 'e' };
	final static char[] FALSE_CHARS = { 'f', 'a', 'l', 's', 'e' };
	final static char[] NULL_CHARS = { 'n', 'u', 'l', 'l' };
	final Buffer input;
	final Buffer overflow;

	public JsonParser(Models context, Buffer input, Buffer overflow, ParserFilterFactory... filters) {
		super(context, filters);
		this.input = input;
		this.overflow = overflow;
	}

	@Override
	public Object parse() {
		switch (input.seek()) {
			case '{':
				return parser.parseObject(models.mapModel);
			case '[':
				return parser.parseList(models.listModel);
			case '"':
				return parser.parseString().toString();
			case 't':
			case 'f':
				return parser.parseBoolean();
			case 'n':
				return parser.parseNull();
			default:
				return models.numberModel.parse(parser);
		}
	}

	@Override
	public Object parseObject(ObjectModel model) {
		final Buffer input = this.input;
		final Buffer overflow = this.overflow;
		final Parser parser= this.parser;
		final Object maker = objectMaker(model);
		var n = input.seek('{');
		if(n == 'n') {
			return parseNull();
		}
		if(n != '{') {
			throw new ModelException("Unexpected character " + n + " in " + input.describe());
		}
		while (true) {
			switch (input.seek('}')) {
			case '"':
				var fn =CharSequenceParser.parse(input, overflow);
				Field f = parser.parseObjectField(model, fn);
				if (f != null) {
					f.parse(maker, parser);
				} else {
					parser.parse();
				}
				break;
			case '}':
				return model.make(maker);
			default:
				throw new ModelException("Unexpected character " + input.seek() + " in " + input.describe());
			}
		}
	}

	@Override
	public Field parseObjectField(
			ObjectModel model, CharSequence field) {
		Field mfield = model.getField(field);
		input.expect(':');
		return mfield;
	}

	@Override
	public  Object parseList(ListModel definition) {
		final Object maker = listMaker(definition);
		final var input = this.input;
		final var t = this.parser;
		var n = input.seek('[');
		if(n == 'n') {
			return parseNull();
		}
		if(n != '[') {
			throw new ModelException("Unexpected character " + n + " in " + input.describe());
		}
		while (true) {
			switch (input.seek(']')) {
			case ']':
				return definition.make(maker);
			default:
				definition.parseItem(maker, t);
			}
		}
	}

	@Override
	public Object parseNull() {
		input.expect(new char[] {'n', 'u', 'l', 'l'});
		return null;
	}

	@Override
	public int parseInt() {
		// CPD-OFF
		var input = this.input;
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
					throw new NumberFormatException("Number format error at " + input.describe());
				}
				break;
			// whitespace chomping
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
				if(ip == rp) {
					ip++;
					break;
				}
			default:
				if (rp == ip || rp == ip + 1 && negative) {
					throw new NumberFormatException("Number format error at " + input.describe());
				}
				input.readPosition = rp;
				return negative ? 0 - value : value;
			}
		}
		// buffer reload needed
		ip = rp - ip;
		rp = 0;
		int offset = ip;
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
					if (rp == ip && offset == 0) {
						negative = true;
					} else {
						throw new NumberFormatException("Number format error at " + input.describe());
					}
					break;
				// whitespace chomping
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if(offset == 0 && ip == rp) {
						ip++;
						break;
					}
				default:
					if (ip == rp && offset == 0 || rp == ip + offset + 1 && negative) {
						throw new NumberFormatException("Number format error at " + input.describe());
					}
					break PARSE_LOOP;
				}
			}
			input.readPosition = rp;
		}
		return negative ? 0 - value : value;
		// CPD-ON
	}

	@Override
	public long parseLong() {
		var input = this.input;
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
					throw new NumberFormatException("Number format error at " + input.describe());
				}
				break;
			// whitespace chomping
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
				if(ip == rp) {
					ip++;
					break;
				}
			default:
				if (rp == ip || rp == ip + 1 && negative) {
					throw new NumberFormatException("Number format error at " + input.describe());
				}
				input.readPosition = rp;
				return negative ? 0 - value : value;
			}
		}
		// buffer reload needed
		ip = rp - ip;
		rp = 0;
		int offset = ip;
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
					if (rp == ip && offset == 0) {
						negative = true;
					} else {
						throw new NumberFormatException("Number format error at " + input.describe());
					}
					break;
				// whitespace chomping
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if(offset == 0 && ip == rp) {
						ip++;
						break;
					}
				default:
					if (ip == rp && offset == 0 || rp == ip + offset + 1 && negative) {
						throw new NumberFormatException("Number format error at " + input.describe());
					}
					break PARSE_LOOP;
				}
			}
			input.readPosition = rp;
		}
		return negative ? 0 - value : value;
	}

	@Override
	public float parseFloat() {
		return (float) parseDouble();
	}

	@Override
	public double parseDouble() {
		var input = this.input;
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
		// CPD-OFF
		int ip = rp;
		// first loop / happy path
		for (; rp < wp; rp++) {
			//System.out.println("Parse loop 1 "+rp+" "+String.valueOf((char) buf[rp]));
			switch (buf[rp]) {
			// whitespace chomping
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
				if(ip == rp) {
					ip++;
					break;
				}
			case ',':
			case ']':
			case '}':
				input.readPosition = rp;
				return FloatingParser.parseNumber(buf, ip, rp-ip);
			default:
				break;
			}
		}
		char[] overflowBuf = overflow.buffer;
		System.arraycopy(buf, ip, overflowBuf, 0, rp - ip);
		// buffer reload needed
		ip = rp - ip;
		rp = 0;
		int offset = ip;
		if (input.refill()) {
			wp = input.writePosition;
			// second loop; any valid number would only span a single buffer boundary
			PARSE_LOOP:
			for (; rp < wp; rp++) {
				//System.out.println("Parse loop 2 "+rp+" "+String.valueOf((char) buf[rp]));
				switch (buf[rp]) {
				// whitespace chomping
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if(offset == 0 && ip == rp) {
						ip++;
						break;
					}
				case ',':
				case ']':
				case '}':
					input.readPosition = rp;
					break PARSE_LOOP;
				default:
					break;
				}
			}
			if(rp > 0) {
				input.readPosition = rp;
				System.arraycopy(buf, 0, overflowBuf, ip, rp);
			}
		}
		// CPD-ON
		return FloatingParser.parseNumber(overflowBuf, 0, rp+ip);
	}

	@Override
	public boolean parseBoolean() {
		var input = this.input;
		switch (input.seek()) {
			case 't':
				input.expect(TRUE_CHARS);
				return true;
			case 'f':
				input.expect(FALSE_CHARS);
				return false;
			default:
				throw new ModelException("Unexpected input for boolean " + input.describe());
		}
	}

	@Override
	public CharSequence parseString() {
		var input = this.input;
		switch (input.seek()) {
			case 'n':
				input.expect(NULL_CHARS);
				return null;
			case '"':
				return CharSequenceParser.parse(input, overflow);
			default:
				throw new ModelException("Expected starting quote "+input.describe());
		}
	}

}