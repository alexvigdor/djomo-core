/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;

import com.bigcloud.djomo.internal.CharArraySequence;

public abstract class DateTimeParser {
	/**
	 * The number of days in a 400 year cycle.
	 */
	private static final int DAYS_PER_CYCLE = 146097;
	/**
	 * The number of days from year zero to year 1970. There are five 400 year
	 * cycles from year zero to 2000. There are 7 leap years from 1970 to 2000.
	 */
	static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

	public int year;
	public int month;
	public int day;
	public int hour;
	public int minute;
	public int seconds;
	public int nanos;
	public ZoneOffset zoneOffset;
	protected int nextChar;
	protected int nextPos;
	protected int readDigits;
	protected final CharSequence sequence;
	protected final int length;

	public DateTimeParser(CharSequence sequence) {
		this.sequence = sequence;
		this.length = sequence.length();
	}

	abstract int readNum();

	abstract void load();

	public static DateTimeParser parser(CharSequence sequence) {
		if (sequence instanceof CharArraySequence seq) {
			return new CharArraySequenceParser(seq);
		}
		return new CharSequenceParser(sequence);
	}

	public OffsetDateTime getOffsetDateTime() {
		parseOffsetDateTime();
		return OffsetDateTime.of(year, month, day, hour, minute, seconds, nanos, zoneOffset);
	}

	public LocalDateTime getLocalDateTime() {
		parseLocalDateTime();
		return LocalDateTime.of(year, month, day, hour, minute, seconds, nanos);
	}

	public LocalDate getLocalDate() {
		parseLocalDate();
		return LocalDate.of(year, month, day);
	}

	public LocalTime getLocalTime() {
		parseLocalTime();
		return LocalTime.of(hour, minute, seconds, nanos);
	}

	public Instant getInstant() {
		parseOffsetDateTime();
		// step 1, calculate epoch day
		long y = year;
		long m = month;
		long total = 0;
		total += 365 * y;
		if (y >= 0) {
			total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
		} else {
			total -= y / -4 - y / -100 + y / -400;
		}
		total += ((367 * m - 362) / 12);
		total += day - 1;
		if (m > 2) {
			total--;
			if (IsoChronology.INSTANCE.isLeapYear(y) == false) {
				total--;
			}
		}
		long epochDay = total - DAYS_0000_TO_1970;
		long secs = epochDay * 86400 + hour * 3600 + minute * 60 + seconds - zoneOffset.getTotalSeconds();
		return Instant.ofEpochSecond(secs, nanos);
	}

	public final void parseLocalDate() {
		if (length - nextPos < 10) {
			throwBadFormat();
		}
		load();
		boolean ne = nextChar == '-';
		boolean po = nextChar == '+';
		if (ne || po) {
			load();
		}
		year = readNum();
		if (ne) {
			year = -year;
		}
		if (nextChar != '-') {
			throwBadFormat();
		}
		load();
		month = readNum();
		if (nextChar != '-') {
			throwBadFormat();
		}
		load();
		day = readNum();
	}

	public final void parseLocalTime() {
		if (length - nextPos < 5) {
			throwBadFormat();
		}
		load();
		hour = readNum();
		if (nextChar != ':') {
			throwBadFormat();
		}
		load();
		minute = readNum();
		if (nextChar == ':') {
			load();
			seconds = readNum();
			if (nextChar == '.') {
				load();
				int nanos = readNum();
				int reorder = 9 - readDigits;
				// adjust nano scale
				switch (reorder) {
				case 1:
					nanos *= 10;
					break;
				case 2:
					nanos *= 100;
					break;
				case 3:
					nanos *= 1000;
					break;
				case 4:
					nanos *= 10000;
					break;
				case 5:
					nanos *= 100000;
					break;
				case 6:
					nanos *= 1000000;
					break;
				case 7:
					nanos *= 10000000;
					break;
				case 8:
					nanos *= 100000000;
					break;
				}
				this.nanos = nanos;
			}
		}
	}

	public final void parseLocalDateTime() {
		parseLocalDate();
		var c = nextChar;
		if (c != 'T' && c != ' ') {
			throwBadFormat();
		}
		parseLocalTime();
	}

	public final void parseZoneOffset() {
		if (length - nextPos < 1) {
			zoneOffset = ZoneOffset.UTC;
			return;
		}
		load();
		var nc = nextChar;
		if (nc == 'Z') {
			zoneOffset = ZoneOffset.UTC;
			return;
		}
		boolean ne = nc == '-';
		boolean po = nc == '+';
		if (ne || po) {
			load();
		}
		var offsetHour = readNum();
		if (ne) {
			offsetHour = -offsetHour;
		}
		if (nextChar == ':') {
			load();
			var offsetMinutes = readNum();
			if (ne) {
				offsetMinutes = -offsetMinutes;
			}
			if (nextChar == ':') {
				load();
				var offsetSeconds = readNum();
				if (ne) {
					offsetSeconds = -offsetSeconds;
				}
				zoneOffset = ZoneOffset.ofHoursMinutesSeconds(offsetHour, offsetMinutes, offsetSeconds);
			} else {
				zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinutes);
			}
		} else {
			if (offsetHour > 10000 || offsetHour < -10000) {
				var offsetSeconds = offsetHour % 100;
				offsetHour /= 100;
				var offsetMinutes = offsetHour % 100;
				offsetHour /= 100;
				zoneOffset = ZoneOffset.ofHoursMinutesSeconds(offsetHour, offsetMinutes, offsetSeconds);
			} else if (offsetHour > 100 || offsetHour < -100) {
				var offsetMinutes = offsetHour % 100;
				offsetHour /= 100;
				zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinutes);
			} else {
				zoneOffset = ZoneOffset.ofHours(offsetHour);
			}
		}
	}

	public final void parseOffsetDateTime() {
		parseLocalDateTime();
		nextPos--;
		parseZoneOffset();
	}

	private void throwBadFormat() {
		throw new IllegalArgumentException("Invalid date format " + sequence);
	}

	protected static class CharSequenceParser extends DateTimeParser {

		protected CharSequenceParser(CharSequence seq) {
			super(seq);
		}

		@Override
		final void load() {
			nextChar = sequence.charAt(nextPos++);
		}

		@Override
		final int readNum() {
			int nc = -1;
			int num = 0;
			int digits = 0;
			char c = (char) nextChar;
			int pos = nextPos;
			int strlen = length;
			var seq = sequence;

			READ: while (true) {
				switch (c) {
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
					digits++;
					num = num * 10 + c - 48;
					break;
				default:
					nc = c;
					break READ;
				}
				if (pos == strlen) {
					break;
				}
				c = seq.charAt(pos++);
			}
			readDigits = digits;
			nextPos = pos;
			nextChar = nc;
			return num;
		}

	}

	protected static class CharArraySequenceParser extends DateTimeParser {
		final private char[] buffer;
		final private int start;

		protected CharArraySequenceParser(CharArraySequence seq) {
			super(seq);
			this.buffer = seq.buffer.buffer;
			this.start = seq.start;
		}

		@Override
		final void load() {
			nextChar = buffer[start + nextPos++];
		}

		@Override
		final int readNum() {
			var buf = buffer;
			int nc = -1;
			int num = 0;
			int digits = 0;
			char c = (char) nextChar;
			int pos = nextPos;
			int strlen = length;
			int st = start;
			READ: while (true) {
				switch (c) {
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
					digits++;
					num = num * 10 + c - 48;
					break;
				default:
					nc = c;
					break READ;
				}
				if (pos == strlen) {
					break;
				}
				c = buf[st + pos++];
			}
			readDigits = digits;
			nextPos = pos;
			nextChar = nc;
			return num;
		}

	}
}
