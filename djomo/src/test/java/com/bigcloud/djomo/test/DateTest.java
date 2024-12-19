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

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.parsers.ModelParser;
import com.bigcloud.djomo.api.visitors.ModelVisitor;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.filter.parsers.FieldParser;
import com.bigcloud.djomo.filter.visitors.FieldVisitor;
import com.bigcloud.djomo.simple.DateFormatModelFactory;

public class DateTest {
	@Test
	public void testDate() throws IOException {
		Date date = new Date();
		Json json = new Json();
		String str = json.toString(date);
		Assert.assertEquals(str, date.getTime() + "");
		Date rt = json.fromString(str, Date.class);
		Assert.assertEquals(rt, date);
	}

	@Test
	public void testDateConvert() {
		Models models = new Models();
		Model<Date> dateModel = models.get(Date.class);
		Date converted = dateModel.convert(null);
		Assert.assertNull(converted);
		long time = System.currentTimeMillis();
		converted = dateModel.convert(time);
		Assert.assertEquals(converted.getTime(), time);
		converted = dateModel.convert(time + "");
		Assert.assertEquals(converted.getTime(), time);
		converted = dateModel.convert(new Date(time));
		Assert.assertEquals(converted.getTime(), time);
	}

	@Test
	public void testDateService() throws IOException {
		Models models = Models.builder()
				.loadFactories()
				.build();
		Json json = new Json(models);
		ZonedDateTime zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		Date date = Date.from(zdt.toInstant());
		String str = json.toString(date);
		Assert.assertEquals(str, "\"" + DateTimeFormatter.ISO_INSTANT.format(zdt) + "\"");
		Date rt = json.fromString(str, Date.class);
		Assert.assertEquals(rt, date);
	}

	@Test
	public void testDateFormat() throws IOException {
		Models models = Models.builder()
				.factory(new DateFormatModelFactory(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL), Date.class))
				.build();
		Json json = new Json(models);
		Date date = Date.from(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant());
		String str = json.toString(date);
		Assert.assertNotEquals(str, date.getTime() + "");
		Date rt = json.fromString(str, Date.class);
		Assert.assertEquals(rt, date);
	}

	@Test
	public void testFormatConvert() {
		Models models = Models.builder()
				.factory(new DateFormatModelFactory(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL), Date.class))
				.build();
		Date zdt = models.get(Date.class).convert(null);
		Assert.assertNull(zdt);
		zdt = models.get(Date.class).convert(new Date());
		Assert.assertNotNull(zdt);
		zdt = models.get(Date.class)
				.convert(ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)));
		Assert.assertNotNull(zdt);
	}

	@Test(expectedExceptions = ModelException.class)
	public void testBadFormatConvert() {
		Models models = Models.builder()
				.factory(new DateFormatModelFactory(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT), Date.class))
				.build();
		models.get(Date.class)
				.convert(ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)));
	}

	@Test
	public void testCustomDateInlineFilter() throws IOException {
		Json json = new Json();
		List<Date> dates = List.of(new Date(123456789), new Date(1233456798));
		String str = json.toString(dates);
		Assert.assertEquals(str, "[123456789,1233456798]");
		List<Date> roundTrip = json.fromString(str, new StaticType<List<Date>>() {
		});
		assertEquals(roundTrip, dates);
		json = Json.builder()
				.visit(Filters.visitModel(Date.class,
						(date, model, visitor) -> visitor.visitString(date.toInstant().toString())))
				.parse(Filters.parseModel(Date.class,
						(model, parser) -> Date.from(Instant.parse(parser.parseString()))))
				.build();
		str = json.toString(dates);
		Assert.assertEquals(str, "[\"1970-01-02T10:17:36.789Z\",\"1970-01-15T06:37:36.798Z\"]");
		roundTrip = json.fromString(str, new StaticType<List<Date>>() {
		});
		assertEquals(roundTrip, dates);
		String d1 = json.toString(dates.get(0));
		Assert.assertEquals(d1, "\"1970-01-02T10:17:36.789Z\"");
	}

	public static class DateVisitor implements ModelVisitor<Date> {

		@Override
		public void visitModel(Date date, Model<Date> model, Visitor visitor) {
			visitor.visitString(date.toInstant().toString());
		}

	}

	public static class DateParser implements ModelParser<Date> {

		@Override
		public Date parse(Model<Date> model, Parser parser) {
			return Date.from(Instant.parse(parser.parseString()));
		}

	}

	@Test
	public void testCustomDateClassFilter() throws IOException {
		Json json = new Json();
		List<Date> dates = List.of(new Date(123456789), new Date(1233456798));
		String str = json.toString(dates);
		Assert.assertEquals(str, "[123456789,1233456798]");
		List<Date> roundTrip = json.fromString(str, new StaticType<List<Date>>() {});
		assertEquals(roundTrip, dates);
		json = Json.builder()
				.visit(new DateVisitor())
				.parse(new DateParser())
				.build();
		str = json.toString(dates);
		Assert.assertEquals(str, "[\"1970-01-02T10:17:36.789Z\",\"1970-01-15T06:37:36.798Z\"]");
		roundTrip = json.fromString(str, new StaticType<List<Date>>() {});
		assertEquals(roundTrip, dates);
		String d1 = json.toString(dates.get(0));
		Assert.assertEquals(d1, "\"1970-01-02T10:17:36.789Z\"");
	}

	@Test
	public void testCustomDateModel() throws IOException {
		List<Date> dates = List.of(new Date(123456789), new Date(1233456798));
		Json json = Json.builder()
				.models(Models.builder()
						.model(Date.class,
								(date, visitor) -> visitor.visitString(date.toInstant().toString()),
								parser -> Date.from(Instant.parse(parser.parseString())))
						.build())
				.build();
		String str = json.toString(dates);
		Assert.assertEquals(str, "[\"1970-01-02T10:17:36.789Z\",\"1970-01-15T06:37:36.798Z\"]");
		List<Date> roundTrip = json.fromString(str, new StaticType<List<Date>>() {});
		assertEquals(roundTrip, dates);
		String d1 = json.toString(dates.get(0));
		Assert.assertEquals(d1, "\"1970-01-02T10:17:36.789Z\"");
		Date rd = json.models().get(Date.class).convert(dates.get(0).toInstant().toString());
		assertEquals(rd, dates.get(0));
	}

	public static record Interval(Date start, Date end) {}

	@Test
	public void testCustomDateFieldInlineFilter() throws IOException {
		Json json = Json.builder()
				.visit(new FieldVisitor<Interval>("end", Filters.visitModel(Date.class,
						(obj, model, visitor) -> visitor.visitString(obj.toInstant().toString()))) {})
				.parse(new FieldParser<Interval>("end", Filters.parseModel(Date.class,
						(model, parser) -> Date.from(Instant.parse(parser.parseString())))) {})
				.build();
		Interval iv = new Interval(new Date(1234567890123l), new Date(1237890123456l));
		String str = json.toString(iv);
		Assert.assertEquals(str, "{\"end\":\"2009-03-24T10:22:03.456Z\",\"start\":1234567890123}");
		Interval roundTrip = json.fromString(str, Interval.class);
		assertEquals(roundTrip, iv);
	}

	@Test
	public void testCustomDateFieldClassFilter() throws IOException {
		Json json = Json.builder()
				.visit(new FieldVisitor<>(Interval.class, "end", new DateVisitor()))
				.parse(new FieldParser<>(Interval.class, "end", new DateParser()))
				.build();
		Interval iv = new Interval(new Date(1234567890123l), new Date(1237890123456l));
		String str = json.toString(iv);
		Assert.assertEquals(str, "{\"end\":\"2009-03-24T10:22:03.456Z\",\"start\":1234567890123}");
		Interval roundTrip = json.fromString(str, Interval.class);
		assertEquals(roundTrip, iv);
	}

}
