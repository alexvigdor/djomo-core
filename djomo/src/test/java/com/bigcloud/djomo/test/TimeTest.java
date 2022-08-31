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
package com.bigcloud.djomo.test;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;

import lombok.Builder;
import lombok.Value;

public class TimeTest {
	Json Json = new Json();
	@Test
	public void testTime() throws IOException {
		TimeBean bean = TimeBean.builder()
				.duration(Duration.of(17, ChronoUnit.MINUTES))
				.period(Period.ofDays(2))
				.localDate(LocalDate.now())
				.localDateTime(LocalDateTime.now())
				.localTime(LocalTime.now())
				.monthDay(MonthDay.now())
				.offsetDateTime(OffsetDateTime.now())
				.offsetTime(OffsetTime.now())
				.year(Year.now())
				.yearMonth(YearMonth.now())
				.zonedDateTime(ZonedDateTime.now())
				.zoneId(ZoneId.systemDefault())
				.zoneOffset(ZoneOffset.ofHours(-7))
				.build();
		String json = Json.toString(bean,"  ");
		TimeBean rt = Json.fromString(json, TimeBean.class);
		Assert.assertEquals(rt, bean);
	}
	
	@Value
	@Builder
	public static class TimeBean{
		//TemporalAmount
		Duration duration;
		Period period;
		// ??
		LocalDate localDate;
		LocalDateTime localDateTime;
		LocalTime localTime;
		MonthDay monthDay;
		OffsetDateTime offsetDateTime;
		OffsetTime offsetTime;
		Year year;
		YearMonth yearMonth;
		ZonedDateTime zonedDateTime;
		ZoneId zoneId;
		ZoneOffset zoneOffset;
	}
}
