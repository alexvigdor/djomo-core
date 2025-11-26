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
import java.time.temporal.TemporalAccessor;

import com.bigcloud.djomo.json.SafeCharSequence;

public abstract class DateTimePrinter<T extends TemporalAccessor> implements SafeCharSequence {
	final T time;

	DateTimePrinter(T time) {
		this.time = time;
	}

	@Override
	public char charAt(int index) {
		// won't be invoked during normal serialization
		return toString().charAt(index);
	}


	@Override
	public CharSequence subSequence(int start, int end) {
		// won't be invoked during normal serialization
		return toString().subSequence(start, end);
	}

	public String toString() {
		// won't be invoked during normal serialization
		return time.toString();
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
	
	//max-length 9
	protected final int printOffset(char[] buf, int pos, int totalSeconds) {
		if(totalSeconds == 0) {
			buf[pos] = 'Z';
			return pos+1;
		}
		if(totalSeconds < 0) {
			totalSeconds = -totalSeconds;
			buf[pos++] = '-';
		}
		else {
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
		if(seconds > 0) {
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

	public static class LocalTimePrinter extends DateTimePrinter<LocalTime> {

		LocalTimePrinter(LocalTime time) {
			super(time);
		}

		@Override
		public int getChars(char[] buf, int pos) {
			var time = this.time;
			return printLocalTime(buf, pos, time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
		}

		@Override
		public int length() {
			return 18;
		}

	}

	public static class LocalDatePrinter extends DateTimePrinter<LocalDate> {

		LocalDatePrinter(LocalDate time) {
			super(time);
		}
		@Override
		public int getChars(char[] buf, int pos) {
			var time = this.time;
			return printLocalDate(buf, pos, time.getYear(), time.getMonthValue(), time.getDayOfMonth());
		}

		@Override
		public int length() {
			return 16;
		}

	}

	public static class LocalDateTimePrinter extends DateTimePrinter<LocalDateTime> {

		LocalDateTimePrinter(LocalDateTime time) {
			super(time);
		}

		@Override
		public int getChars(char[] buf, int pos) {
			var time = this.time;
			pos = printLocalDate(buf, pos, time.getYear(), time.getMonthValue(), time.getDayOfMonth());
			buf[pos++] = 'T';
			return printLocalTime(buf, pos, time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
		}

		@Override
		public int length() {
			return 35;
		}

	}

	public static class OffsetDateTimePrinter extends DateTimePrinter<OffsetDateTime> {
		
		public OffsetDateTimePrinter(OffsetDateTime time) {
			super(time);
		}
		
		@Override
		public int getChars(char[] buf, int pos) {
			var time = this.time;
			var lt = time.toLocalDateTime();
			var localDate = lt.toLocalDate();
			var localTime = lt.toLocalTime();
			pos = printLocalDate(buf, pos, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
			buf[pos++] = 'T';
			pos = printLocalTime(buf, pos, localTime.getHour(), localTime.getMinute(), localTime.getSecond(), localTime.getNano());
			return printOffset(buf, pos, time.getOffset().getTotalSeconds());
		}

		@Override
		public int length() {
			return 44;
		}

	}
	
	public static class InstantDateTimePrinter extends DateTimePrinter<Instant> {
		
		InstantDateTimePrinter(Instant time) {
			super(time);
		}
		
		@Override
		public int getChars(char[] buf, int pos) {
			var time = this.time;
			long second = time.getEpochSecond();
			int nanos = time.getNano();
			var localDate = LocalDate.ofEpochDay(Math.floorDiv(second, 86400));
			var localTime = LocalTime.ofNanoOfDay(Math.floorMod(second, 86400) * 1000_000_000l + nanos);
			pos = printLocalDate(buf, pos, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
			buf[pos++] = 'T';
			pos = printLocalTime(buf, pos, localTime.getHour(), localTime.getMinute(), localTime.getSecond(), localTime.getNano());
			buf[pos++] = 'Z';
			return pos;
		}

		@Override
		public int length() {
			return 36;
		}

	}

	
}
