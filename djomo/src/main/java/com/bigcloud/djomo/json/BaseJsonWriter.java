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
		int p = reserve(1);
		buffer[p] = c;
		pos = p + 1;
	}

	protected final void append(char... chars) {
		int cl = chars.length;
		int p = reserve(cl);
		System.arraycopy(chars, 0, buffer, p, cl);
		pos = p + cl;
	}

	@Override
	public void visitNull() {
		append('n', 'u', 'l', 'l');
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
			pos = scanAndEscapeChars(buf, lpos, len);
			append('"');
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
		var p = pos;
		int room = BUF_LEN - p;
		if (room < 11) {
			sink.next(p);
			p = 0;
		}
		char[] buf = buffer;
		switch (value) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
			buf[p] = (char) ('0' + value);
			pos = p + 1;
			break;
		case Integer.MIN_VALUE:
			String.valueOf(Integer.MIN_VALUE).getChars(0, 11, buf, p);
			pos = p + 11;
			break;
		default:
			boolean negative = false;
			int normal = value;
			if (value < 0) {
				negative = true;
				normal = -normal;
			}
			int stringLen = normal < 100000
					? normal < 100 ? normal < 10 ? 1 : 2 : normal < 1000 ? 3 : normal < 10000 ? 4 : 5
					: normal < 10000000 ? normal < 1000000 ? 6 : 7
							: normal < 100000000 ? 8 : normal < 1000000000 ? 9 : 10;

			if (negative) {
				buf[p] = '-';
				++stringLen;
			}
			pos = p += stringLen;
			do {
				buf[--p] = (char) (48 + (normal % 10));
				normal /= 10;
			} while (normal != 0);
		}
	}

	@Override
	public void visitLong(long value) {
		if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
			visitInt((int) value);
			return;
		}
		var p = pos;
		int room = BUF_LEN - p;
		if (room < 20) {
			sink.next(p);
			p = 0;
		}
		char[] buf = buffer;
		if (value == Long.MIN_VALUE) {
			String.valueOf(Long.MIN_VALUE).getChars(0, 20, buf, p);
			pos = p + 20;
			return;
		}
		boolean negative = false;
		long normal = value;
		if (value < 0) {
			negative = true;
			normal = -normal;
		}
		// we are above 2147483647 or below -2147483648
		int stringLen = normal < 100000000000000l
				? normal < 100000000000l ? normal < 10000000000l ? 10 : 11
						: normal < 1000000000000l ? 12 : normal < 10000000000000l ? 13 : 14
				: normal < 10000000000000000l ? normal < 1000000000000000l ? 15 : 16
						: normal < 100000000000000000l ? 17 : normal < 1000000000000000000l ? 18 : 19;

		if (negative) {
			buf[p] = '-';
			++stringLen;
		}
		pos = p += stringLen;
		do {
			buf[--p] = (char) (48 + (normal % 10));
			normal /= 10;
		} while (normal != 0);
	}

	@Override
	public void visitFloat(float value) {
		var p = pos;
		int room = BUF_LEN - p;
		if (room < 15) {
			sink.next(p);
			p = 0;
		}
		pos = FloatPrinter.printFloat(value, buffer, p);
	}

	@Override
	public void visitDouble(double value) {
		var p = pos;
		int room = BUF_LEN - p;
		if (room < 24) {
			sink.next(p);
			p = 0;
		}
		pos = DoublePrinter.printDouble(value, buffer, p);
	}

	@Override
	public void visitBoolean(boolean value) {
		if (value) {
			append('t', 'r', 'u', 'e');
		} else {
			append('f', 'a', 'l', 's', 'e');
		}
	}

	@Override
	public <T extends TemporalAccessor> void visitTemporal(T time) {
		var p = pos;
		int room = BUF_LEN - p;
		if (room < 80) {
			sink.next(p);
			p = 0;
		}
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
