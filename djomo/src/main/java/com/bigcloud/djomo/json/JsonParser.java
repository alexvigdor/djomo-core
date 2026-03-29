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
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.api.TemporalType;
import com.bigcloud.djomo.base.BaseParser;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.internal.CharArraySequence;
import com.bigcloud.djomo.internal.FloatingParser;


public class JsonParser extends BaseParser implements Parser {
	private static final ThreadLocal<char[]> localInput = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[16384];
		}
	};

	private static final ThreadLocal<char[]> localOverflow = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[4096];
		}
	};
	 // Scale to 9 digits: index 0 (0 digits) to index 9 (9 digits)
	private static final int[] SCALE = { 0, 100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1 };
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
			for (; rp < wp; rp++) {
				char c = buf[rp];
				if (c > ' ' && c != ',') {
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
		//we have to account for the possibility the encoded data is longer than an int
		return (int) parseLong();
	}

	@Override
	public long parseLong() {
		char c = seek();
		boolean negative = c == '-';
		int pos = readPosition;
		long val = 0;
		// Fast path: digits are already in the current buffer
		if (pos + 21 < inputLength) {
			char[] b = input;
			if (negative) {
				c = b[++pos];
			}
			for (int i = 0; i < 20; i++) {
				c = (char) (c - '0');
				if (c >= 10) {
					break;
				}
				val = val * 10 + c;
				c = b[++pos];
			}
			readPosition = pos;
		} else {
			val = parseSlow(21, '-');
		}
		if (negative) {
			val = -val;
		}
		return val;
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

	@Override
	public <T extends TemporalAccessor> T parseTemporal(TemporalType<T> type) {
		char next =	seek();
		if(next != '"') {
			throw new ModelException("Expected starting quote " + describe());
		}
		readPosition++;
		var builder = TemporalType.builder();
		char c = peek();
		if(((char) (c - '0')) < 10) {
			parseStandardDate(builder);
		}
		else if(c == '+') {
			++readPosition;
			parseExtendedDate(builder, 1);
		}
		else if(c == '-') {
			++readPosition;
			if (peek() == '-') {
				++readPosition;
				parseMonthDay(builder);
			}
			else {
				// If not --, it's a negative Year
				parseExtendedDate(builder, -1);
			}
		}
		else if(c == 'T') {
			builder.format = TemporalType.Format.TIME;
			parseTime(builder);
		}
		else if(c=='Z'){
			builder.format = TemporalType.Format.OFFSET;
			builder.isUtc = true;
		}
		else {
			throw new ModelException("Unexpected date/time format " + describe());
		}
		if(peek() == '"') {
			readPosition++;
		}
		else {
			throw new ModelException("Expected ending quote " + describe());
		}
		return type.from(builder);
	}

	private void parseMonthDay(TemporalType.Builder res) {
		res.month = parseByte(-1);
		res.day = parseByte('-');
		res.format = TemporalType.Format.MONTH_DAY;
	}

	private void parseStandardDate(TemporalType.Builder res) {
		res.year = parseInt(4);
		if ((res.month = parseByte('-')) == 0) {
			if (peek() == ':') {
				// Special case: input was actually HH:mm (Year was 2 digits)
				parseTimeFromYear(res);
			} else {
				res.format = TemporalType.Format.YEAR;
			}
		}
		else {
			parseDayTime(res);
		}
	}

	private void parseTimeFromYear(TemporalType.Builder res) {
		res.hour = (byte) res.year;
		res.year = 0;
		res.format = TemporalType.Format.TIME;
		res.minute = parseByte(':');
		res.second = parseByte(':');
		parseNanos(res);
		parseZone(res);
	}

	private void parseExtendedDate(TemporalType.Builder res, int sign) {
		res.year = sign * parseInt(10);
		if ((res.month = parseByte('-')) == 0) {
			if (peek() == ':') {
				// Special case: input was actually +HH:mm or -HHmmss
				res.format = TemporalType.Format.OFFSET;
				parseZone(res, res.year);
				res.year = 0;
			} else {
				res.format = TemporalType.Format.YEAR;
			}
		} else {
			parseDayTime(res);
		}
	}

	private void parseDayTime(TemporalType.Builder res) {
		if ((res.day = parseByte('-')) == 0) {
			res.format = TemporalType.Format.YEAR_MONTH;
		} else {
			// 3. Time Part
			char t = peek();
			if (t == 'T') {
				res.format = TemporalType.Format.DATE_TIME;
				parseTime(res);
			} else {
				res.format = TemporalType.Format.DATE;
			}
		}
	}

	private void parseTime(TemporalType.Builder res) {
		res.hour = parseByte('T');
		res.minute = parseByte(':');
		res.second = parseByte(':');

		// Sub-seconds
		parseNanos(res);
		parseZone(res);
	}

	private void parseNanos(TemporalType.Builder res) {
		char dot = peek();
		if (dot == '.') {
			int st = ++readPosition;
			int value = parseInt(9);
			int count = readPosition - st;
			if (count < 0) {
				// buffer overflow correction
				count += input.length;
			}
			res.nanos = value * SCALE[count];
		}
	}

	private void parseZone(TemporalType.Builder res) {
		int sign = 0;
		char off = peek();
		if (off == 'Z') {
			res.isUtc = true;
			++readPosition;
		} else if (off == '+') {
			sign = 1;
		} else if (off == '-') {
			sign = -1;
		}
		if (sign != 0) {
			byte h = parseByte(off);
			byte m = parseByte(':');
			byte s = parseByte(':');
			res.offsetTotalSeconds = sign * (h * 3600 + m * 60 + s);
			if (res.format == TemporalType.Format.TIME) {
				res.format = TemporalType.Format.OFFSET_TIME;
			}
		}
		parseZoneId(res);
	}

	private void parseZoneId(TemporalType.Builder res) {
		int rp = readPosition;
		int wp = inputLength;
		if (rp == wp) {
			if (refill()) {
				rp = 0;
				wp = inputLength;
			} else {
				return;
			}
		}
		char[] buf = input;
		if (buf[rp] == '[') {
			int start = ++rp;
			while (rp < wp) {
				if (buf[rp++] == ']') {
					res.zoneId = new String(buf, start, rp - start - 1);
					readPosition = rp;
					return;
				}
			}
			var ov = overflow;
			// buffer overflow
			int op = rp - start;
			System.arraycopy(buf, start, ov, 0, op);
			if (refill()) {
				rp = 0;
				wp = inputLength;
				char c;
				while (rp < wp) {
					c = buf[rp++];
					if (c == ']') {
						res.zoneId = new String(ov, 0, op);
						readPosition = rp;
						return;
					}
					ov[op++] = c;
				}
			}
			throw new ModelException("Expected closing ] " + describe());
		}
	}

	private void parseZone(TemporalType.Builder res, int parsedHour) {
		int sign = parsedHour > 0 ? 1 : -1;
		parsedHour *= sign;
		int h = parsedHour < 99 ? parsedHour : parsedHour < 9999 ? parsedHour/100 : parsedHour/10000;
		int m = parsedHour < 99 ? parseByte(':') : parsedHour < 9999 ? parsedHour % 100 : parsedHour / 100 % 100;
		int s = parsedHour < 9999 ? parseByte(':') : parsedHour % 10;
		res.offsetTotalSeconds = sign * (h * 3600 + m * 60 + s);
		parseZoneId(res);
	}

	private int parseInt(int max) {
		int pos = readPosition;
		// Fast path: digits are already in the current buffer
		if (pos + max < inputLength) {
			char[] b = input;
			char c = b[pos];
			int val = 0;
			for (int i = 0; i < max; i++) {
				c = (char) (c - '0');
				if(c >= 10) {
					break;
				}
				val = val * 10 + c;
				c = b[++pos];
			}
			readPosition = pos;
			return val;
		} else {
			return (int) parseSlow(max, -1);
		}
	}

	private byte parseByte(int skipFirst) {
		int pos = readPosition;
		if (pos + 3 < inputLength) {
			char[] b = input;
			char c = b[pos];
			if (c == skipFirst) {
				c = b[++pos];
			}
			c = (char) (c - '0');
			if(c >= 10) {
				return 0;
			}
			byte val = (byte) c;
			c = (char) (b[++pos] - '0');
			if(c < 10) {
				++pos;
				val = (byte) (10 * val + c);
			}
			readPosition = pos;
			return val;
		}
		else {
			return (byte) parseSlow(2, skipFirst);
		}
	}

	private long parseSlow(int max, int skipFirst) {
		char c = peek();
		if (c == skipFirst) {
			++readPosition;
			c = peek();
		}
		long val = 0;
		// Slow path: component crosses buffer boundary
		for (int i = 0; i < max; i++) {
			c = (char) (c - '0');
			if(c >= 10) {
				break;
			}
			val = val * 10 + c;
			++readPosition;
			c = peek();
		}
		return val;
	}

	private char peek() {
		if (readPosition >= inputLength && !refill())
			return '\0';
		return input[readPosition];
	}

}