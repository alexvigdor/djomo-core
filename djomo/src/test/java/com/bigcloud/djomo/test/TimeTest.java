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
import java.time.Duration;
import java.time.Instant;
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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.simple.DateFormatModelFactory;

import lombok.Builder;
import lombok.Value;

public class TimeTest {
	Json Json = new Json();

	@Test
	public void testTime() throws IOException {
		TimeBean bean = bean();
		String json = Json.toString(bean, "  ");
		TimeBean rt = Json.fromString(json, TimeBean.class);
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testFormat() throws IOException {
		TimeBean bean = bean();
		String plain = Json.toString(bean, "  ");
		var full = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
		var medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
		Json custom = new Json(Models.builder()
				.model(ZonedDateTime.class,
						(zdt, visitor) -> visitor.visitString(full.format(zdt)),
						parser -> full.parse(parser.parseString(), ZonedDateTime::from)
				)
				.model(LocalDate.class,
						(zdt, visitor) -> visitor.visitString(medium.format(zdt)),
						parser -> medium.parse(parser.parseString(), LocalDate::from)
				)
				.build());
		String formatted = custom.toString(bean);
		Assert.assertNotEquals(formatted, plain);
		TimeBean rt = custom.fromString(formatted, TimeBean.class);
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testConvert() {
		Models models = new Models();
		Instant instant = models.get(Instant.class).convert(null);
		Assert.assertNull(instant);
		instant = models.get(Instant.class).convert(Instant.now());
		Assert.assertNotNull(instant);
		instant = models.get(Instant.class).convert(Instant.now().toString());
		Assert.assertNotNull(instant);
	}

	@Test(expectedExceptions = ModelException.class)
	public void testBadConvert() {
		Models models = new Models();
		models.get(Instant.class).convert(ZonedDateTime.now());
	}

	@Test
	public void testFormatConvert() {
		Models models = Models.builder()
				.factory(new DateFormatModelFactory(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL),
						ZonedDateTime.class))
				.build();
		ZonedDateTime zdt = models.get(ZonedDateTime.class).convert(null);
		Assert.assertNull(zdt);
		zdt = models.get(ZonedDateTime.class).convert(ZonedDateTime.now());
		Assert.assertNotNull(zdt);
		zdt = models.get(ZonedDateTime.class)
				.convert(ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)));
		Assert.assertNotNull(zdt);
	}

	@Test(expectedExceptions = ModelException.class)
	public void testBadFormatConvert() {
		Models models = Models.builder()
				.factory(new DateFormatModelFactory(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL),
						ZonedDateTime.class))
				.build();
		models.get(ZonedDateTime.class).convert(ZonedDateTime.now().toString());
	}

	@Test(expectedExceptions = ModelException.class)
	public void testBadTypeConvert() {
		Models models = Models.builder()
				.factory(new DateFormatModelFactory(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL),
						ZonedDateTime.class))
				.build();
		models.get(ZonedDateTime.class).convert(Instant.now());
	}

	private static TimeBean bean() {
		return TimeBean.builder()
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
				.zonedDateTime(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
				.zoneId(ZoneId.systemDefault())
				.zoneOffset(ZoneOffset.ofHours(-7))
				.build();
	}

	@Value
	@Builder
	public static class TimeBean {
		// TemporalAmount
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
