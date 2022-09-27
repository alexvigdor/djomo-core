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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.error.ModelException;
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
}
