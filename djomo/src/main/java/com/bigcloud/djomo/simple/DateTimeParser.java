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
	protected int nextPos;
	protected final CharSequence sequence;
	protected final int length;

	public DateTimeParser(CharSequence sequence) {
		this.sequence = sequence;
		this.length = sequence.length();
	}

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

	public abstract void parseLocalDate();

	public abstract void parseLocalTime();

	public abstract void parseLocalDateTime();

	public abstract void parseZoneOffset();

	public final void parseOffsetDateTime() {
		parseLocalDateTime();
		parseZoneOffset();
	}

	protected void throwBadFormat() {
		throw new IllegalArgumentException("Invalid date format " + sequence);
	}

	protected static class CharSequenceParser extends DateTimeParser {

		protected CharSequenceParser(CharSequence seq) {
			super(seq);
		}

		public final void parseLocalDateTime() {
			parseLocalDate();
			var len = length;
			var pos = nextPos;
			if (pos < len) {
				char c = sequence.charAt(pos);
				if (c != 'T' && c != ' ') {
					throwBadFormat();
				}
				nextPos = pos + 1;
				parseLocalTime();
			}
		}

		public final void parseLocalTime() {
			var len = length;
			var pos = nextPos;
			if (len - pos < 5) {
				throwBadFormat();
			}
			var seq = sequence;
			int hour = seq.charAt(pos++) - 48;
			hour = hour * 10 + seq.charAt(pos++) - 48;
			if (seq.charAt(pos++) != ':') {
				throwBadFormat();
			}
			this.hour = hour;
			int minute = seq.charAt(pos++) - 48;
			minute = minute * 10 + seq.charAt(pos++) - 48;
			this.minute = minute;
			if (pos < len && seq.charAt(pos) == ':') {
				++pos;
				int seconds = seq.charAt(pos++) - 48;
				seconds = seconds * 10 + seq.charAt(pos++) - 48;
				this.seconds = seconds;
				if (pos < len && seq.charAt(pos) == '.') {
					++pos;
					char nc = seq.charAt(pos);
					if (nc >= '0' && nc <= '9') {
						int nanos = 100000000 * (nc - 48);
						nc = seq.charAt(++pos);
						if (nc >= '0' && nc <= '9') {
							nanos += 10000000 * (nc - 48);
							nc = seq.charAt(++pos);
							if (nc >= '0' && nc <= '9') {
								nanos += 1000000 * (nc - 48);
								nc = seq.charAt(++pos);
								if (nc >= '0' && nc <= '9') {
									nanos += 100000 * (nc - 48);
									nc = seq.charAt(++pos);
									if (nc >= '0' && nc <= '9') {
										nanos += 10000 * (nc - 48);
										nc = seq.charAt(++pos);
										if (nc >= '0' && nc <= '9') {
											nanos += 1000 * (nc - 48);
											nc = seq.charAt(++pos);
											if (nc >= '0' && nc <= '9') {
												nanos += 100 * (nc - 48);
												nc = seq.charAt(++pos);
												if (nc >= '0' && nc <= '9') {
													nanos += 10 * (nc - 48);
													nc = seq.charAt(++pos);
													if (nc >= '0' && nc <= '9') {
														nanos += nc - 48;
														++pos;
													}
												}
											}
										}
									}
								}
							}
						}
						this.nanos = nanos;
					}

				}
			}
			this.nextPos = pos;
		}

		public final void parseLocalDate() {
			var len = length;
			var pos = nextPos;
			if (len - pos < 10) {
				throwBadFormat();
			}
			var seq = sequence;
			char nc = seq.charAt(pos++);
			boolean ne = nc == '-';
			boolean po = nc == '+';
			if (ne || po) {
				nc = seq.charAt(pos++);
			}
			int year = 0;
			while (nc != '-') {
				year = year * 10 + nc - 48;
				nc = seq.charAt(pos++);
			}
			if (ne) {
				year = -year;
			}
			this.year = year;
			int month = seq.charAt(pos++) - 48;
			month = month * 10 + seq.charAt(pos++) - 48;
			this.month = month;
			if (seq.charAt(pos++) != '-') {
				throwBadFormat();
			}
			int day = seq.charAt(pos++) - 48;
			day = day * 10 + seq.charAt(pos++) - 48;
			this.day = day;
			this.nextPos = pos;
		}

		public final void parseZoneOffset() {
			var len = length;
			var pos = nextPos;
			if (len - pos < 2) {
				zoneOffset = ZoneOffset.UTC;
				return;
			}
			var seq = sequence;
			char nc = seq.charAt(pos++);
			if (nc == 'Z') {
				zoneOffset = ZoneOffset.UTC;
				return;
			}
			boolean ne = nc == '-';
			boolean po = nc == '+';
			if (ne || po) {
				nc = seq.charAt(pos++);
			}
			var offsetHour = nc - 48;
			if (pos < len) {
				offsetHour = offsetHour * 10 + seq.charAt(pos++) - 48;
				if (ne) {
					offsetHour = -offsetHour;
				}
				if (pos < len) {
					nc = seq.charAt(pos++);
					if (nc == ':') {
						nc = seq.charAt(pos++);
					}
					var offsetMinutes = nc - 48;
					offsetMinutes = offsetMinutes * 10 + seq.charAt(pos++) - 48;
					if (ne) {
						offsetMinutes = -offsetMinutes;
					}
					if (pos < len) {
						nc = seq.charAt(pos++);
						if (nc == ':') {
							nc = seq.charAt(pos++);
						}
						var offsetSeconds = nc - 48;
						offsetSeconds = offsetSeconds * 10 + seq.charAt(pos++) - 48;
						if (ne) {
							offsetSeconds = -offsetSeconds;
						}
						zoneOffset = ZoneOffset.ofHoursMinutesSeconds(offsetHour, offsetMinutes, offsetSeconds);
					} else {
						zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinutes);
					}
				} else {
					zoneOffset = ZoneOffset.ofHours(offsetHour);
				}
			} else {
				if (ne) {
					offsetHour = -offsetHour;
				}
				zoneOffset = ZoneOffset.ofHours(offsetHour);
			}
		}

	}

	protected static class CharArraySequenceParser extends DateTimeParser {
		final private char[] buffer;
		final private int start;

		protected CharArraySequenceParser(CharArraySequence seq) {
			super(seq);
			this.buffer = seq.buffer;
			this.start = seq.start;
		}

		public final void parseLocalDateTime() {
			parseLocalDate();
			var len = length;
			var pos = nextPos;
			if (pos < len) {
				char c = buffer[start + pos];
				if (c != 'T' && c != ' ') {
					throwBadFormat();
				}
				nextPos = pos + 1;
				parseLocalTime();
			}
		}

		public final void parseLocalTime() {
			var len = length;
			var pos = nextPos;
			if (len - pos < 5) {
				throwBadFormat();
			}
			int st = start;
			var buf = buffer;
			int hour = buf[st + pos++] - 48;
			hour = hour * 10 + buf[st + pos++] - 48;
			if (buf[st + pos++] != ':') {
				throwBadFormat();
			}
			this.hour = hour;
			int minute = buf[st + pos++] - 48;
			minute = minute * 10 + buf[st + pos++] - 48;
			this.minute = minute;
			if (pos < len && buf[st + pos] == ':') {
				++pos;
				int seconds = buf[st + pos++] - 48;
				seconds = seconds * 10 + buf[st + pos++] - 48;
				this.seconds = seconds;
				if (pos < len && buf[st + pos] == '.') {
					++pos;
					char nc = buf[st + pos];
					if (nc >= '0' && nc <= '9') {
						int nanos = 100000000 * (nc - 48);
						nc = buf[st + ++pos];
						if (nc >= '0' && nc <= '9') {
							nanos += 10000000 * (nc - 48);
							nc = buf[st + ++pos];
							if (nc >= '0' && nc <= '9') {
								nanos += 1000000 * (nc - 48);
								nc = buf[st + ++pos];
								if (nc >= '0' && nc <= '9') {
									nanos += 100000 * (nc - 48);
									nc = buf[st + ++pos];
									if (nc >= '0' && nc <= '9') {
										nanos += 10000 * (nc - 48);
										nc = buf[st + ++pos];
										if (nc >= '0' && nc <= '9') {
											nanos += 1000 * (nc - 48);
											nc = buf[st + ++pos];
											if (nc >= '0' && nc <= '9') {
												nanos += 100 * (nc - 48);
												nc = buf[st + ++pos];
												if (nc >= '0' && nc <= '9') {
													nanos += 10 * (nc - 48);
													nc = buf[st + ++pos];
													if (nc >= '0' && nc <= '9') {
														nanos += nc - 48;
														++pos;
													}
												}
											}
										}
									}
								}
							}
						}
						this.nanos = nanos;
					}

				}
			}
			this.nextPos = pos;
		}

		public final void parseLocalDate() {
			var len = length;
			var pos = nextPos;
			if (len - pos < 10) {
				throwBadFormat();
			}
			int st = start;
			var buf = buffer;
			char nc = buf[st + pos++];
			boolean ne = nc == '-';
			boolean po = nc == '+';
			if (ne || po) {
				nc = buf[st + pos++];
			}
			int year = 0;
			while (nc != '-') {
				year = year * 10 + nc - 48;
				nc = buf[st + pos++];
			}
			this.year = year;
			int month = buf[st + pos++] - 48;
			month = month * 10 + buf[st + pos++] - 48;
			this.month = month;
			if (buf[st + pos++] != '-') {
				throwBadFormat();
			}
			int day = buf[st + pos++] - 48;
			day = day * 10 + buf[st + pos++] - 48;
			this.day = day;
			this.nextPos = pos;
		}

		public final void parseZoneOffset() {
			var len = length;
			var pos = nextPos;
			if (len - pos < 2) {
				zoneOffset = ZoneOffset.UTC;
				return;
			}
			int st = start;
			var buf = buffer;
			char nc = buf[st + pos++];
			if (nc == 'Z') {
				zoneOffset = ZoneOffset.UTC;
				return;
			}
			boolean ne = nc == '-';
			boolean po = nc == '+';
			if (ne || po) {
				nc = buf[st + pos++];
			}
			var offsetHour = nc - 48;
			if (pos < len) {
				offsetHour = offsetHour * 10 + buf[st + pos++] - 48;
				if (ne) {
					offsetHour = -offsetHour;
				}
				if (pos < len) {
					nc = buf[st + pos++];
					if (nc == ':') {
						nc = buf[st + pos++];
					}
					var offsetMinutes = nc - 48;
					offsetMinutes = offsetMinutes * 10 + buf[st + pos++] - 48;
					if (ne) {
						offsetMinutes = -offsetMinutes;
					}
					if (pos < len) {
						nc = buf[st + pos++];
						if (nc == ':') {
							nc = buf[st + pos++];
						}
						var offsetSeconds = nc - 48;
						offsetSeconds = offsetSeconds * 10 + buf[st + pos++] - 48;
						if (ne) {
							offsetSeconds = -offsetSeconds;
						}
						zoneOffset = ZoneOffset.ofHoursMinutesSeconds(offsetHour, offsetMinutes, offsetSeconds);
					} else {
						zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinutes);
					}
				} else {
					zoneOffset = ZoneOffset.ofHours(offsetHour);
				}
			} else {
				if (ne) {
					offsetHour = -offsetHour;
				}
				zoneOffset = ZoneOffset.ofHours(offsetHour);
			}
			nextPos = pos;
		}
	}
}
