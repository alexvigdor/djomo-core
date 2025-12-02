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
import java.io.Reader;
import java.util.Arrays;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.base.BaseParser;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.internal.CharArraySequence;
import com.bigcloud.djomo.internal.FloatingParser;

public class JsonParser extends BaseParser implements Parser {
	private static final ThreadLocal<char[]> localInput = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[8192];
		}
	};

	private static final ThreadLocal<char[]> localOverflow = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[4096];
		}
	};
	final char[] input = localInput.get();
	final char[] overflow = localOverflow.get();
	protected final CharArraySequence charArraySequence = new CharArraySequence(input);
	protected final Reader source;
	protected int readPosition;
	protected int inputLength;

	public JsonParser(Models context, Reader source, ParserFilterFactory... filters) {
		super(context, filters);
		this.source = source;
	}

	public boolean refill() {
		try {
			inputLength = source.read(input);
		} catch (IOException e) {
			throw new ModelException("Error reading input", e);
		}
		if (inputLength == -1) {
			return false;
		}
		readPosition = 0;
		return true;
	}

	public void refillStrict() {
		if (!refill()) {
			throw new ModelException("Unexpected EOF");
		}
	}

	protected final char seek() {
		int rp = readPosition;
		var buf = input;
		while (true) {
			int wp = inputLength;
			while (rp < wp) {
				char c = buf[rp];
				switch (c) {
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
					return c;
				}
			}
			refillStrict();
			rp = 0;
		}
	}

	protected final String describe() {
		int start = readPosition - 10;
		if (start < 0) {
			start = 0;
		}
		int end = start + 20;
		if (end > inputLength) {
			end = inputLength;
		}
		if (end < start) {
			end = start;
		}
		return String.valueOf(input, start, end - start);
	}

	protected final void expect(char... target) {
		int rp = readPosition;
		var buf = input;
		int tp = 0;
		while (true) {
			int wp = inputLength;
			while (rp < wp) {
				char c = buf[rp++];
				switch (c) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if (tp == 0) {
						break;
					}
				default:
					if (c != target[tp++]) {
						throw new ModelException("Expected " + new String(target) + " but found " + describe());
					}
					if (tp == target.length) {
						readPosition = rp;
						return;
					}
				}
			}
			refillStrict();
			rp = 0;
		}
	}

	@Override
	public Object parse() {
		int rp = readPosition;
		var buf = input;
		char c;
		while (true) {
			int wp = inputLength;
			while (rp < wp) {
				c = buf[rp];
				switch (c) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
					++rp;
					break;
				case '{':
					readPosition = rp;
					return parser.parseObject(models.mapModel);
				case '[':
					readPosition = rp;
					return parser.parseList(models.listModel);
				case '"':
					readPosition = rp;
					return parser.parseString().toString();
				case 't':
				case 'f':
					readPosition = rp;
					return parser.parseBoolean();
				case 'n':
					readPosition = rp;
					return parser.parseNull();
				default:
					readPosition = rp;
					return models.numberModel.parse(parser);
				}
			}
			refillStrict();
			rp = 0;
		}
	}

	@Override
	public Object parseObject(ObjectModel model) {
		final Parser parser = this.parser;
		final CharArraySequence cas = this.charArraySequence;
		int rp = readPosition;
		var buf = input;
		boolean inObject = false;
		final Object maker = objectMaker(model);
		Field field = null;
		CharSequence fieldName = null;
		while (true) {
			int wp = inputLength;
			while (rp < wp) {
				char c = buf[rp];
				switch (c) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
					++rp;
					break;
				case 'n':
					return parseNull();
				case ':':
					readPosition = rp + 1;
					if (field != null) {
						field.parse(maker, parser);
					} else {
						parser.parse();
					}
					rp = readPosition;
					break;
				case '"':
					fieldName = null;
					int strPos = ++rp;
					for (; strPos < wp; strPos++) {
						c = buf[strPos];
						if (c == '"') {
							cas.start = rp;
							cas.len = strPos - rp;
							fieldName = cas;
							rp = ++strPos;
							break;
						}
						if (c == '\\') {
							// pessimistic fallback
							break;
						}
					}
					if (fieldName == null) {
						fieldName = parseCharSequence(rp, strPos);
						rp = readPosition;
					} else {
						readPosition = rp;
					}
					field = parser.parseObjectField(model, fieldName);
					break;
				case '{':
					if (!inObject) {
						inObject = true;
						++rp;
						break;
					}
					throwUnexpected(c);
				case '}':
					if (inObject) {
						readPosition = rp + 1;
						return model.make(maker);
					}
				default:
					throwUnexpected(c);
				}
			}
			refillStrict();
			rp = 0;
		}
	}

	private void throwUnexpected(char c) {
		throw new ModelException("Unexpected character " + c + " in " + describe());
	}

	@Override
	public Field parseObjectField(
			ObjectModel model, CharSequence field) {
		Field mfield = model.getField(field);
		return mfield;
	}

	@Override
	public Object parseList(ListModel definition) {
		final Object maker = listMaker(definition);
		final var input = this.input;
		final var t = this.parser;
		int rp = readPosition;
		var buf = input;
		boolean inList = false;
		while (true) {
			int wp = inputLength;
			while (rp < wp) {
				char c = buf[rp];
				switch (c) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
					++rp;
					break;
				case ']':
					readPosition = rp + 1;
					if (inList) {
						return definition.make(maker);
					}
					throwUnexpected(c);
				case '[':
					if (!inList) {
						++rp;
						inList = true;
						break;
					}
				case 'n':
					if (!inList) {
						readPosition = rp;
						return parseNull();
					}
				default:
					readPosition = rp;
					definition.parseItem(maker, t);
					rp = readPosition;
				}
			}
			refillStrict();
			rp = 0;
		}
	}

	@Override
	public Object parseNull() {
		expect(new char[] { 'n', 'u', 'l', 'l' });
		return null;
	}

	@Override
	public int parseInt() {
		// CPD-OFF
		final var buf = input;
		int rp = readPosition;
		int wp = inputLength;
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
				value = value * 10 + ch - 48;
				break;
			case '-':
				if (rp == ip) {
					negative = true;
				} else {
					throw new NumberFormatException("Number format error at " + describe());
				}
				break;
			// whitespace chomping
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
				if (ip == rp) {
					ip++;
					break;
				}
			default:
				if (rp == ip || rp == ip + 1 && negative) {
					throw new NumberFormatException("Number format error at " + describe());
				}
				readPosition = rp;
				return negative ? 0 - value : value;
			}
		}
		// buffer reload needed
		int offset = rp - ip;
		boolean done = false;
		rp = 0;
		while (!done && refill()) {
			wp = inputLength;
			// second loop; any valid number would only span a single buffer boundary
			PARSE_LOOP: for (rp = ip = 0; rp < wp; rp++) {
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
					value = value * 10 + ch - 48;
					break;
				case '-':
					if (rp == ip && offset == 0) {
						negative = true;
					} else {
						throw new NumberFormatException("Number format error at " + describe());
					}
					break;
				// whitespace chomping
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if (offset == 0 && ip == rp) {
						ip++;
						break;
					}
				default:
					if (offset == 0 && (ip == rp || rp == ip + 1 && negative)) {
						throw new NumberFormatException("Number format error at " + describe());
					}
					done = true;
					break PARSE_LOOP;
				}
			}
			offset += rp - ip;
		}
		readPosition = rp;
		return negative ? 0 - value : value;
		// CPD-ON
	}

	@Override
	public long parseLong() {
		final var buf = input;
		int rp = readPosition;
		int wp = inputLength;
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
				value = value * 10 + ch - 48;
				break;
			case '-':
				if (rp == ip) {
					negative = true;
				} else {
					throw new NumberFormatException("Number format error at " + describe());
				}
				break;
			// whitespace chomping
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
				if (ip == rp) {
					ip++;
					break;
				}
			default:
				if (rp == ip || rp == ip + 1 && negative) {
					throw new NumberFormatException("Number format error at " + describe());
				}
				readPosition = rp;
				return negative ? 0 - value : value;
			}
		}
		// buffer reload needed
		int offset = rp - ip;
		boolean done = false;
		rp = 0;
		while (!done && refill()) {
			wp = inputLength;
			PARSE_LOOP: for (rp = ip = 0; rp < wp; rp++) {
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
					value = value * 10 + ch - 48;
					break;
				case '-':
					if (rp == ip && offset == 0) {
						negative = true;
					} else {
						throw new NumberFormatException("Number format error at " + describe());
					}
					break;
				// whitespace chomping
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if (offset == 0 && ip == rp) {
						ip++;
						break;
					}
				default:
					if (offset == 0 && (ip == rp || rp == ip + 1 && negative)) {
						throw new NumberFormatException("Number format error at " + describe());
					}
					done = true;
					break PARSE_LOOP;
				}
			}
			offset += rp - ip;
		}
		readPosition = rp;
		return negative ? 0 - value : value;
	}

	@Override
	public float parseFloat() {
		return (float) parseDouble();
	}

	@Override
	public double parseDouble() {
		final var buf = input;
		int rp = readPosition;
		int wp = inputLength;
		if (rp == wp) {
			if (!refill()) {
				throw new NumberFormatException("Number Model incomplete at " + describe());
			}
			rp = 0;
			wp = inputLength;
		}
		// CPD-OFF
		int ip = rp;
		// first loop / happy path
		for (; rp < wp; rp++) {
			// System.out.println("Parse loop 1 "+rp+" "+String.valueOf((char) buf[rp]));
			switch (buf[rp]) {
			// whitespace chomping
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
				if (ip == rp) {
					ip++;
					break;
				}
			case ',':
			case ']':
			case '}':
				readPosition = rp;
				return FloatingParser.parseNumber(buf, ip, rp - ip);
			default:
				break;
			}
		}
		char[] overflowBuf = overflow;
		int opos = rp - ip;
		System.arraycopy(buf, ip, overflowBuf, 0, opos);
		// buffer reload needed
		boolean done = false;
		while (!done && refill()) {
			wp = inputLength;
			PARSE_LOOP: for (rp = ip = 0; rp < wp; rp++) {
				switch (buf[rp]) {
				// whitespace chomping
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
					if (opos == 0 && ip == rp) {
						ip++;
						break;
					}
				case ',':
				case ']':
				case '}':
					done = true;
					break PARSE_LOOP;
				default:
					break;
				}
			}
			if (rp > ip) {
				int len = rp - ip;
				System.arraycopy(buf, ip, overflowBuf, opos, len);
				opos += len;
			}
		}
		readPosition = rp;
		// CPD-ON
		return FloatingParser.parseNumber(overflowBuf, 0, opos);
	}

	@Override
	public boolean parseBoolean() {
		switch (seek()) {
		case 't':
			expect(new char[] { 't', 'r', 'u', 'e' });
			return true;
		case 'f':
			expect(new char[] { 'f', 'a', 'l', 's', 'e' });
			return false;
		default:
			throw new ModelException("Unexpected input for boolean " + describe());
		}
	}

	@Override
	public CharSequence parseString() {
		int rp = readPosition;
		var buf = input;
		while (true) {
			int wp = inputLength;
			while (rp < wp) {
				char c = buf[rp];
				switch (c) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
					++rp;
					break;
				case 'n':
					return (CharSequence) parseNull();
				case '"':
					int strPos = ++rp;
					for (; strPos < wp; strPos++) {
						c = buf[strPos];
						if (c == '"') {
							CharArraySequence cas = this.charArraySequence;
							cas.start = rp;
							cas.len = strPos - rp;
							readPosition = ++strPos;
							return cas;
						}
						if (c == '\\') {
							// pessimistic fallback
							break;
						}
					}
					return parseCharSequence(rp, strPos);
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
				case '-':
					readPosition = rp;
					return parseNumericString();
				default:
					throw new ModelException("Expected starting quote " + describe());
				}
			}
			refillStrict();
			rp = 0;
		}
	}

	private CharSequence parseNumericString() {
		final var buf = input;
		int rp = readPosition;
		int wp = inputLength;
		// CPD-OFF
		int ip = rp;
		// first loop / happy path
		for (; rp < wp; rp++) {
			// System.out.println("Parse loop 1 "+rp+" "+String.valueOf((char) buf[rp]));
			switch (buf[rp]) {
			case ' ':
			case '\t':
			case '\n':
			case '\r':
			case '\f':
			case ',':
			case ']':
			case '}':
				readPosition = rp;
				var seq = charArraySequence;
				seq.start = ip;
				seq.len = rp - ip;
				return seq;
			default:
				break;
			}
		}
		char[] overflowBuf = overflow;
		System.arraycopy(buf, ip, overflowBuf, 0, rp - ip);
		// buffer reload needed
		ip = rp - ip;
		rp = 0;
		if (refill()) {
			wp = inputLength;
			// second loop; any valid number would only span a single buffer boundary
			PARSE_LOOP: for (; rp < wp; rp++) {
				// System.out.println("Parse loop 2 "+rp+" "+String.valueOf((char) buf[rp]));
				switch (buf[rp]) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case '\f':
				case ',':
				case ']':
				case '}':
					readPosition = rp;
					break PARSE_LOOP;
				default:
					break;
				}
			}
			if (rp > 0) {
				readPosition = rp;
				System.arraycopy(buf, 0, overflowBuf, ip, rp);
			}
		}
		var seq = new CharArraySequence(overflowBuf);
		seq.start = 0;
		seq.len = rp + ip;
		return seq;
	}

	protected final CharSequence parseCharSequence(int inStart, int inBreak) {
		var output = overflow;
		int outputLength = overflow.length;
		var input = this.input;
		int inputLength = this.inputLength;
		int writePos = inBreak - inStart;
		if (writePos > 0) {
			if (writePos > outputLength) {
				outputLength = expandBuffer(outputLength, writePos, false);
				output = Arrays.copyOf(output, outputLength);
			}
			System.arraycopy(input, inStart, output, 0, writePos);
		}
		int rp = inStart = inBreak;
		OUTER: while (true) {
			if (rp == inputLength) {
				if (rp > inStart) {
					int len = rp - inStart;
					int newPos = writePos + len;
					if (newPos > outputLength) {
						outputLength = expandBuffer(outputLength, newPos, false);
						output = Arrays.copyOf(output, outputLength);
					}
					System.arraycopy(input, inStart, output, writePos, rp - inStart);
					writePos = newPos;
				}
				refillStrict();
				rp = inStart = 0;
				inputLength = this.inputLength;
			}
			char r = input[rp++];
			if (r == '"') {
				if (rp - 1 > inStart) {
					int len = rp - inStart - 1;
					int newPos = writePos + len;
					if (newPos > outputLength) {
						outputLength = expandBuffer(outputLength, newPos, true);
						output = Arrays.copyOf(output, outputLength);
					}
					System.arraycopy(input, inStart, output, writePos, len);
					writePos = newPos;
				}
				readPosition = rp;
				var seq = new CharArraySequence(output);
				seq.start = 0;
				seq.len = writePos;
				return seq;
			}
			if (r == '\\') {
				if (rp > inStart) {
					int len = rp - inStart - 1;
					int newPos = writePos + len;
					if (newPos > outputLength) {
						outputLength = expandBuffer(outputLength, newPos, false);
						output = Arrays.copyOf(output, outputLength);
					}
					System.arraycopy(input, inStart, output, writePos, len);
					writePos = newPos;
				}
				if (rp == inputLength) {
					refillStrict();
					rp = inStart = 0;
					inputLength = this.inputLength;
				}
				if (outputLength < writePos + 1) {
					outputLength = expandBuffer(outputLength, outputLength + 1, false);
					output = Arrays.copyOf(output, outputLength);
				}
				r = input[rp++];
				int result = switch (r) {
				case 'n' -> '\n';
				case 'r' -> '\r';
				case 't' -> '\t';
				case 'f' -> '\f';
				case 'b' -> '\b';
				case 'u' -> {
					int accum = 0;
					boolean ec6 = false;
					int chars = 0;
					ESCAPE: while (true) {
						if (rp == inputLength) {
							refillStrict();
							rp = inStart = 0;
							inputLength = this.inputLength;
						}
						r = input[rp++];
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
							inStart = --rp;
							yield -1;
						}
						accum = accum * 16 + p;
						if (!ec6 && ++chars == 4) {
							break;
						}
					}
					yield (char) accum;
				}
				default -> r;
				};
				if (result != -1) {
					output[writePos++] = (char) result;
				}
				inStart = rp;
			}
			for (; rp < inputLength; rp++) {
				r = input[rp];
				if (r == '"' || r == '\\') {
					break;
				}
			}
		}
	}

	protected int expandBuffer(int curSize, int targetSize, boolean terminal) {
		if (targetSize > 268435456) {
			throw new ModelException("Target string length " + targetSize + " is too large, the input is suspicious");
		}
		if (terminal) {
			return targetSize;
		}
		while (curSize < targetSize) {
			curSize *= 2;
		}
		if (curSize > 268435456) {
			return targetSize;
		}
		return curSize;
	}

}