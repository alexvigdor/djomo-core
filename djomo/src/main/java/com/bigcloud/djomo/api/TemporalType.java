/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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
package com.bigcloud.djomo.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;

public interface TemporalType<T extends TemporalAccessor> {

	T from(Builder dateTime);

	static Builder builder() {
		return new Builder();
	}

	TemporalType<OffsetDateTime> OFFSET_DATE_TIME = new TemporalTypeImpl<>(OffsetDateTime.class) {

		@Override
		public OffsetDateTime from(Builder builder) {
			return builder.toOffsetDateTime();
		}

	};

	TemporalType<LocalDateTime> LOCAL_DATE_TIME = new TemporalTypeImpl<>(LocalDateTime.class) {

		@Override
		public LocalDateTime from(Builder builder) {
			return builder.toLocalDateTime();
		}

	};

	TemporalType<ZonedDateTime> ZONED_DATE_TIME = new TemporalTypeImpl<>(ZonedDateTime.class) {

		@Override
		public ZonedDateTime from(Builder builder) {
			return builder.toZonedDateTime();
		}

	};

	TemporalType<Instant> INSTANT = new TemporalTypeImpl<>(Instant.class) {

		@Override
		public Instant from(Builder builder) {
			return builder.toInstant();
		}

	};

	TemporalType<LocalDate> LOCAL_DATE = new TemporalTypeImpl<>(LocalDate.class) {

		@Override
		public LocalDate from(Builder builder) {
			return builder.toLocalDate();
		}

	};

	TemporalType<LocalTime> LOCAL_TIME = new TemporalTypeImpl<>(LocalTime.class) {

		@Override
		public LocalTime from(Builder builder) {
			return builder.toLocalTime();
		}

	};

	TemporalType<Year> YEAR = new TemporalTypeImpl<>(Year.class) {

		@Override
		public Year from(Builder builder) {
			return builder.toYear();
		}

	};

	TemporalType<YearMonth> YEAR_MONTH = new TemporalTypeImpl<>(YearMonth.class) {

		@Override
		public YearMonth from(Builder builder) {
			return builder.toYearMonth();
		}

	};

	TemporalType<MonthDay> MONTH_DAY = new TemporalTypeImpl<>(MonthDay.class) {

		@Override
		public MonthDay from(Builder builder) {
			return builder.toMonthDay();
		}

	};

	TemporalType<OffsetTime> OFFSET_TIME = new TemporalTypeImpl<>(OffsetTime.class) {

		@Override
		public OffsetTime from(Builder builder) {
			return builder.toOffsetTime();
		}

	};

	TemporalType<ZoneOffset> ZONE_OFFSET = new TemporalTypeImpl<>(ZoneOffset.class) {

		@Override
		public ZoneOffset from(Builder builder) {
			return builder.toZoneOffset();
		}

	};

	TemporalType<TemporalAccessor> DYNAMIC = new TemporalTypeImpl<>(TemporalAccessor.class) {

		@Override
		public TemporalAccessor from(Builder builder) {
			return builder.toTemporalAccessor();
		}

	};

	Class<T> getTemporalClass();

	@SuppressWarnings("unchecked")
	static <T extends TemporalAccessor> TemporalType<T> typeOf(Class<T> temporalClass) {
		var l = TemporalTypeImpl.LOOKUP.get(temporalClass);
		if (l == null) {
			l = DYNAMIC;
		}
		return (TemporalType<T>) l;
	}

	abstract class TemporalTypeImpl<T extends TemporalAccessor> implements TemporalType<T> {
		static final private Map<Class<?>, TemporalType<?>> LOOKUP = new HashMap<>();
		private final Class<T> temporalClass;

		private TemporalTypeImpl(Class<T> temporalClass) {
			this.temporalClass = temporalClass;
			LOOKUP.put(temporalClass, this);
		}

		public Class<T> getTemporalClass() {
			return temporalClass;
		}

		public abstract T from(Builder dateTime);
	}

	enum Format {
		YEAR, YEAR_MONTH, DATE, DATE_TIME, TIME, OFFSET_TIME, MONTH_DAY, OFFSET;
	}

	final class Builder {
		private static final int DAYS_PER_CYCLE = 146097;
		private static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

		public int year, nanos, offsetTotalSeconds;

		public byte month, day, hour, minute, second;

		public Format format;

		public boolean isUtc;

		public String zoneId;

		public OffsetDateTime toOffsetDateTime() {
			return OffsetDateTime.of(year, month, day, hour, minute, second, nanos,
					ZoneOffset.ofTotalSeconds(offsetTotalSeconds));
		}

		public LocalDateTime toLocalDateTime() {
			return LocalDateTime.of(year, month, day, hour, minute, second, nanos);
		}

		public ZonedDateTime toZonedDateTime() {
			return ZonedDateTime.of(year, month, day, hour, minute, second, nanos, ZoneId.of(zoneId));
		}

		public LocalDate toLocalDate() {
			return LocalDate.of(year, month, day);
		}

		public LocalTime toLocalTime() {
			return LocalTime.of(hour, minute, second, nanos);
		}

		public Year toYear() {
			return Year.of(year);
		}

		public YearMonth toYearMonth() {
			return YearMonth.of(year, month);
		}

		public MonthDay toMonthDay() {
			return MonthDay.of(month, day);
		}

		public OffsetTime toOffsetTime() {
			return OffsetTime.of(hour, minute, second, nanos, ZoneOffset.ofTotalSeconds(offsetTotalSeconds));
		}

		public ZoneOffset toZoneOffset() {
			if (isUtc) {
				return ZoneOffset.UTC;
			}
			return ZoneOffset.ofTotalSeconds(offsetTotalSeconds);
		}

		public TemporalAccessor toTemporalAccessor() {
			return switch (format) {
			case DATE -> toLocalDate();
			case DATE_TIME -> isUtc ? toInstant()
					: zoneId != null ? toZonedDateTime()
							: offsetTotalSeconds != 0 ? toOffsetDateTime() : toLocalDateTime();
			case MONTH_DAY -> toMonthDay();
			case OFFSET_TIME -> toOffsetTime();
			case TIME -> toLocalTime();
			case YEAR -> toYear();
			case YEAR_MONTH -> toYearMonth();
			case OFFSET -> toZoneOffset();
			};
		}

		public Instant toInstant() {
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
			long secs = epochDay * 86400 + hour * 3600 + minute * 60 + second - offsetTotalSeconds;
			return Instant.ofEpochSecond(secs, nanos);
		}
	}

}
