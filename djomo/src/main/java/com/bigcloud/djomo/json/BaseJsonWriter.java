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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.VisitorFilterFactory;
import com.bigcloud.djomo.base.BaseVisitor;
import com.bigcloud.djomo.internal.DoublePrinter;
import com.bigcloud.djomo.internal.FloatPrinter;
import com.bigcloud.djomo.io.CharSink;

public abstract class BaseJsonWriter extends BaseVisitor implements AutoCloseable {
	protected static final int BUF_LEN = 4096;
	private static final ThreadLocal<char[]> localBuffer = new ThreadLocal<>() {
		public char[] initialValue() {
			return new char[BUF_LEN];
		}
	};
	private static final ThreadLocal<char[]> localStrBuffer = new ThreadLocal<>() {
		public char[] initialValue() {
			return new char[BUF_LEN];
		}
	};
	private static final char[] MIN_LONG_CHARS = { '-', '9', '2', '2', '3', '3', '7', '2', '0', '3', '6', '8', '5', '4',
			'7', '7', '5', '8', '0', '8' };
	private static final char[] MIN_INT_CHARS = { '-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8' };
	private static final char[] NULL_CHARS = { 'n', 'u', 'l', 'l' };
	private static final char[] TRUE_CHARS = { 't', 'r', 'u', 'e' };
	private static final char[] FALSE_CHARS = { 'f', 'a', 'l', 's', 'e' };
	private static final char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
			'F' };
	private static final boolean[] special = new boolean[65536];
	static {
		Arrays.fill(special, 0, 32, true);
		special['"'] = true;
		special['\\'] = true;
		special['\u2028'] = true;
		special['\u2029'] = true;
	}
	protected final char[] buffer = localBuffer.get();
	protected final char[] strBuffer = localStrBuffer.get();
	protected final CharSink sink;
	int pos = 0;
	boolean first;

	public BaseJsonWriter(Models context, CharSink sink, VisitorFilterFactory... filters) {
		super(context, filters);
		this.sink = sink;
		sink.buffer(buffer);
	}

	protected final int reserve(int len) {
		int p = pos;
		if (BUF_LEN - p < len) {
			sink.next(p);
			// caller will update pos
			p = 0;
		}
		return p;
	}

	protected final void append(char c) {
		int p = pos;
		if (BUF_LEN == p) {
			sink.next(p);
			p = 0;
		}
		buffer[p] = c;
		pos = p + 1;
	}

	protected final void append(char[] chars) {
		int cl = chars.length;
		int p = reserve(cl);
		System.arraycopy(chars, 0, buffer, p, cl);
		pos = p + cl;
	}

	@Override
	public void visitNull() {
		append(NULL_CHARS);
	}

	@Override
	public void visitString(CharSequence str) {
		if (str instanceof SafeCharSequence scs) {
			visitSafeCharSequence(scs);
		} else {
			visitStringFast(str);
		}
	}

	private void visitSafeCharSequence(SafeCharSequence scs) {
		var buf = buffer;
		int lpos = reserve(scs.length() + 2);
		buf[lpos++] = '"';
		lpos = scs.getChars(buf, lpos);
		buf[lpos++] = '"';
		pos = lpos;
	}

	private void visitStringFast(CharSequence str) {
		int len = str.length();
		var buf = buffer;
		int roomNeeded = len + 2;
		if (roomNeeded < BUF_LEN) {
			int lpos = reserve(roomNeeded);
			buf[lpos++] = '"';
			copyStringToBuffer(str, 0, buf, lpos, len);
			lpos = scanAndEscapeChars(buf, lpos, len);
			if (lpos == BUF_LEN) {
				sink.next(lpos);
				lpos = 0;
			}
			buf[lpos++] = '"';
			pos = lpos;
		} else {
			int lpos = pos;
			visitStringLoop(str, len, BUF_LEN - lpos, lpos, buf);
		}
	}

	private void copyStringToBuffer(CharSequence str, int start, char[] buf, int lpos, int len) {
		if (str instanceof String s) {
			s.getChars(start, start + len, buf, lpos);
		} else {
			for (int x = 0; x < len; x++) {
				buf[lpos + x] = str.charAt(x + start);
			}
		}
	}

	private void visitStringLoop(CharSequence str, int len, int room, int lpos, char[] buf) {
		if (room < 2) {
			sink.next(lpos);
			lpos = 0;
			room = BUF_LEN;
		}
		buf[lpos++] = '"';
		int start = 0;
		if (len < room) {
			room = len;
		} else {
			--room;
		}

		while (true) {
			copyStringToBuffer(str, start, buf, lpos, room);
			lpos = scanAndEscapeChars(buf, lpos, room);
			if (lpos == BUF_LEN) {
				sink.next(lpos);
				lpos = 0;
			}
			if ((len -= room) == 0) {
				break;
			}
			start += room;
			room = BUF_LEN - lpos;
			if (len < room) {
				room = len;
			}
		}
		buf[lpos] = '"';
		pos = lpos + 1;
	}

	private int scanAndEscapeChars(char[] buf, int lpos, int len) {
		var spec = special;
		int target = lpos + len;

		// Scan for first special character
		for (; lpos < target; lpos++) {
			if (spec[buf[lpos]]) {
				break;
			}
		}

		// If no special chars found, we're done with this chunk
		if (target == lpos) {
			return lpos;
		}

		// Found special char - delegate to escape handler
		return handleEscapeSequence(buf, lpos, len, lpos - (target - len));
	}

	private int handleEscapeSequence(char[] buf, int lpos, int len, int firstSpecialIdx) {
		var spec = special;
		var sbuf = strBuffer;

		// Handle escaping starting from the first special character
		int c = buf[lpos];
		int p = 0;
		int rem = len - firstSpecialIdx - 1;
		if (rem > 0) {
			System.arraycopy(buf, lpos + 1, sbuf, 0, rem);
		}

		while (true) {
			if (BUF_LEN - lpos < 2) {
				sink.next(lpos);
				lpos = 0;
			}
			buf[lpos++] = '\\';
			lpos = escapeChar(buf, lpos, c);

			// Scan for next special character in sbuf
			int i;
			for (i = p; i < rem; i++) {
				c = sbuf[i];
				if (spec[c]) {
					break;
				}
			}

			if (i > p) {
				int l = i - p;
				if (BUF_LEN - lpos < l) {
					sink.next(lpos);
					lpos = 0;
				}
				System.arraycopy(sbuf, p, buf, lpos, l);
				lpos += l;
			}

			if (i >= rem) {
				break;
			}

			p = i + 1;
		}

		return lpos;
	}

	private int escapeChar(char[] buf, int lpos, int c) {
		switch (c) {
		case '\n':
			buf[lpos++] = 'n';
			break;
		case '\r':
			buf[lpos++] = 'r';
			break;
		case '\t':
			buf[lpos++] = 't';
			break;
		case '\f':
			buf[lpos++] = 'f';
			break;
		case '\b':
			buf[lpos++] = 'b';
			break;
		case '"':
			buf[lpos++] = '"';
			break;
		case '\\':
			buf[lpos++] = '\\';
			break;
		default:
			buf[lpos++] = 'u';
			if (BUF_LEN - lpos < 4) {
				sink.next(lpos);
				lpos = 0;
			}
			final char[] hex = hexchars;
			buf[lpos++] = hex[c / 4096];
			c = c % 4096;
			buf[lpos++] = hex[c / 256];
			c = c % 256;
			buf[lpos++] = hex[c / 16];
			c = c % 16;
			buf[lpos++] = hex[c];
		}
		return lpos;
	}

	@Override
	public void close() {
		sink.last(pos);
	}

	@Override
	public void visitInt(int value) {
		final boolean small = value < 10;
		int length = 0;
		if (small) {
			if (value >= 0) {
				append((char) ('0' + value));
				return;
			}
			if (value == Integer.MIN_VALUE) {
				append(MIN_INT_CHARS);
				return;
			}
			value = -value;
			length = 1;
		}
		length += stringSize(value);
		length += reserve(length);
		pos = length;
		formatIntBytesBackward(value, length, small);
	}

	@Override
	public void visitLong(long value) {
		final boolean small = value < 10;
		int length = 0;
		if (small) {
			if (value >= 0) {
				append((char) ('0' + value));
				return;
			}
			if (value == Long.MIN_VALUE) {
				append(MIN_LONG_CHARS);
				return;
			}
			value = -value;
			length = 1;
		}
		length += stringSizeLong(value);
		length += reserve(length);
		pos = length;
		var buf = buffer;
		while (value > Integer.MAX_VALUE) {
			long q = value / 100;
			int r = (int) (value - (q * 100));
			value = q;
			buf[--length] = DigitOnes[r];
			buf[--length] = DigitTens[r];
		}
		formatIntBytesBackward((int) value, length, small);
	}

	@Override
	public void visitFloat(float value) {
		var p = reserve(15);
		pos = FloatPrinter.printFloat(value, buffer, p);
	}

	@Override
	public void visitDouble(double value) {
		var p = reserve(24);
		pos = DoublePrinter.printDouble(value, buffer, p);
	}

	@Override
	public void visitBoolean(boolean value) {
		if (value) {
			append(TRUE_CHARS);
		} else {
			append(FALSE_CHARS);
		}
	}

	@Override
	public <T extends TemporalAccessor> void visitTemporal(T time) {
		var p = reserve(80);
		var buf = buffer;
		buf[p++] = '"';
		if (time instanceof OffsetDateTime t) {
			p = writeTemporal(t, p);
		} else if (time instanceof Instant t) {
			p = writeTemporal(t, p);
		} else if (time instanceof ZonedDateTime t) {
			p = writeTemporal(t, p);
		} else if (time instanceof LocalDateTime t) {
			p = writeTemporal(t, p);
		} else if (time instanceof LocalDate t) {
			p = writeTemporal(t, p);
		} else if (time instanceof LocalTime t) {
			p = writeTemporal(t, p);
		} else {
			var str = time.toString();
			int len = str.length();
			str.getChars(0, len, buf, p);
			p += len;
		}
		buf[p++] = '"';
		pos = p;
	}

	// CPD-OFF
	private static final char[] DigitTens = {
			'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
			'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
			'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
			'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
			'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
			'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
			'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
			'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
			'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
			'9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
	};

	private static final char[] DigitOnes = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	};
	// CPD-ON

	private void formatIntBytesBackward(int vInt, int writeIdx, boolean negative) {
		var buf = this.buffer;
		while (vInt >= 100) {
			int q = vInt / 100;
			int r = vInt - (q * 100);
			vInt = q;
			buf[--writeIdx] = DigitOnes[r];
			buf[--writeIdx] = DigitTens[r];
		}
		if (vInt >= 10) {
			buf[--writeIdx] = DigitOnes[vInt];
			buf[--writeIdx] = DigitTens[vInt];
		} else {
			buf[--writeIdx] = (char) ('0' + vInt);
		}
		if (negative) {
			buf[--writeIdx] = '-';
		}
	}

	private static final long[] MAX_VALUES_FOR_DIGITS = {
			0L,
			9L,
			99L,
			999L,
			9999L,
			99999L,
			999999L,
			9999999L,
			99999999L,
			999999999L,
			9999999999L,
			99999999999L,
			999999999999L,
			9999999999999L,
			99999999999999L,
			999999999999999L,
			9999999999999999L,
			99999999999999999L,
			999999999999999999L,
			9223372036854775806L
	};

	private static final int[] LZ_TO_MIN_DIGITS_INT = {
			10, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5,
			5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0
	};

	private static final int[] LZ_TO_MIN_DIGITS_LONG = {
			0, 18, 18, 18, 18, 17, 17, 17, 16, 16, 16, 15, 15, 15, 15, 14, 14,
			14, 13, 13, 13, 12, 12, 12, 12, 11, 11, 11, 10, 10, 10, 9, 9, 9,
			9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4,
			3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0
	};

	private static int stringSize(int x) {
		int lz = Integer.numberOfLeadingZeros(x);
		return calculateSize(x, LZ_TO_MIN_DIGITS_INT[lz]);
	}

	private static int stringSizeLong(long x) {
		int lz = Long.numberOfLeadingZeros(x);
		return calculateSize(x, LZ_TO_MIN_DIGITS_LONG[lz]);
	}

	private static int calculateSize(long x, int digits) {
		long maxVal = MAX_VALUES_FOR_DIGITS[digits];
		int correction = (int) ((maxVal - x) >>> 63);
		return digits + correction;
	}

	final int writeTemporal(LocalTime time, int pos) {
		return printLocalTime(buffer, pos, time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
	}

	final int writeTemporal(LocalDate time, int pos) {
		return printLocalDate(buffer, pos, time.getYear(), time.getMonthValue(), time.getDayOfMonth());
	}

	final int writeTemporal(LocalDateTime time, int pos) {
		var buf = buffer;
		pos = printLocalDate(buf, pos, time.getYear(), time.getMonthValue(), time.getDayOfMonth());
		buf[pos++] = 'T';
		return printLocalTime(buf, pos, time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
	}

	final int writeTemporal(OffsetDateTime time, int pos) {
		var buf = buffer;
		var lt = time.toLocalDateTime();
		var localDate = lt.toLocalDate();
		var localTime = lt.toLocalTime();
		pos = printLocalDate(buf, pos, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
		buf[pos++] = 'T';
		pos = printLocalTime(buf, pos, localTime.getHour(), localTime.getMinute(), localTime.getSecond(),
				localTime.getNano());
		return printOffset(buf, pos, time.getOffset().getTotalSeconds());
	}

	final int writeTemporal(ZonedDateTime time, int pos) {
		var buf = buffer;
		var lt = time.toLocalDateTime();
		var localDate = lt.toLocalDate();
		var localTime = lt.toLocalTime();
		pos = printLocalDate(buf, pos, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
		buf[pos++] = 'T';
		pos = printLocalTime(buf, pos, localTime.getHour(), localTime.getMinute(), localTime.getSecond(),
				localTime.getNano());
		pos = printOffset(buf, pos, time.getOffset().getTotalSeconds());
		var zone = time.getZone().toString();
		int zl = zone.length();
		buf[pos++] = '[';
		zone.getChars(0, zl, buf, pos);
		pos += zl;
		buf[pos++] = ']';
		return pos;
	}

	final int writeTemporal(Instant time, int pos) {
		var buf = buffer;
		long second = time.getEpochSecond();
		int nanos = time.getNano();
		var localDate = LocalDate.ofEpochDay(Math.floorDiv(second, 86400));
		var localTime = LocalTime.ofNanoOfDay(Math.floorMod(second, 86400) * 1000_000_000l + nanos);
		pos = printLocalDate(buf, pos, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
		buf[pos++] = 'T';
		pos = printLocalTime(buf, pos, localTime.getHour(), localTime.getMinute(), localTime.getSecond(),
				localTime.getNano());
		buf[pos++] = 'Z';
		return pos;
	}

	// max length 18
	protected final int printLocalTime(char[] buf, int pos, int hour, int minute, int seconds, int nanos) {
		if (hour < 10) {
			buf[pos++] = '0';
			buf[pos++] = (char) (hour + 48);
		} else {
			buf[pos++] = (char) ((hour / 10) + 48);
			buf[pos++] = (char) ((hour % 10) + 48);
		}
		buf[pos++] = ':';
		if (minute < 10) {
			buf[pos++] = '0';
			buf[pos++] = (char) (minute + 48);
		} else {
			buf[pos++] = (char) ((minute / 10) + 48);
			buf[pos++] = (char) ((minute % 10) + 48);
		}
		if (seconds > 0 || nanos > 0) {
			buf[pos++] = ':';
			if (seconds < 10) {
				buf[pos++] = '0';
				buf[pos++] = (char) (seconds + 48);
			} else {
				buf[pos++] = (char) ((seconds / 10) + 48);
				buf[pos++] = (char) ((seconds % 10) + 48);
			}
			if (nanos > 0) {
				buf[pos++] = '.';
				boolean sig = false;
				int sigdigits = 9;
				int c;
				for (int i = 8; i >= 0; i--) {
					c = nanos % 10;
					nanos /= 10;
					if (c == 0 && !sig) {
						--sigdigits;
						continue;
					}
					buf[pos + i] = (char) (c + 48);
					sig = true;
				}
				pos += sigdigits;
			}
		}
		return pos;
	}

	// max-length 16
	protected final int printLocalDate(char[] buf, int pos, int year, int month, int day) {
		if (year < 0) {
			buf[pos++] = '-';
			year = -year;
		} else if (year > 9999) {
			buf[pos++] = '+';
		}
		int digits = 4;
		if (year > 9999) {
			digits = year <= 99999 ? 5 : year <= 999999 ? 6 : year <= 9999999 ? 7 : year <= 99999999 ? 8 : 9;
		}
		for (int i = digits - 1; i >= 0; i--) {
			buf[pos + i] = (char) ((year % 10) + 48);
			year /= 10;
		}
		pos += digits;
		buf[pos++] = '-';
		if (month < 10) {
			buf[pos++] = '0';
			buf[pos++] = (char) (month + 48);
		} else {
			buf[pos++] = '1';
			buf[pos++] = (char) (month + 38);
		}
		buf[pos++] = '-';
		if (day < 10) {
			buf[pos++] = '0';
			buf[pos++] = (char) (day + 48);
		} else {
			buf[pos++] = (char) ((day / 10) + 48);
			buf[pos++] = (char) ((day % 10) + 48);
		}
		return pos;
	}

	// max-length 9
	protected final int printOffset(char[] buf, int pos, int totalSeconds) {
		if (totalSeconds == 0) {
			buf[pos] = 'Z';
			return pos + 1;
		}
		if (totalSeconds < 0) {
			totalSeconds = -totalSeconds;
			buf[pos++] = '-';
		} else {
			buf[pos++] = '+';
		}
		int hours = totalSeconds / 3600;
		if (hours < 10) {
			buf[pos++] = '0';
			buf[pos++] = (char) (hours + 48);
		} else {
			buf[pos++] = (char) ((hours / 10) + 48);
			buf[pos++] = (char) ((hours % 10) + 48);
		}
		int minutes = totalSeconds % 3600 / 60;
		buf[pos++] = ':';
		if (minutes < 10) {
			buf[pos++] = '0';
			buf[pos++] = (char) (minutes + 48);
		} else {
			buf[pos++] = (char) ((minutes / 10) + 48);
			buf[pos++] = (char) ((minutes % 10) + 48);
		}
		int seconds = totalSeconds % 60;
		if (seconds > 0) {
			buf[pos++] = ':';
			if (seconds < 10) {
				buf[pos++] = '0';
				buf[pos++] = (char) (seconds + 48);
			} else {
				buf[pos++] = (char) ((seconds / 10) + 48);
				buf[pos++] = (char) ((seconds % 10) + 48);
			}
		}
		return pos;
	}

}
