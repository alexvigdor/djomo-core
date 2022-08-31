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
import java.net.URI;
import java.net.URL;
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
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;

import lombok.Builder;
import lombok.Value;

public class MagicStringTest {
	Json Json = new Json();
	@Test
	public void testMagicStrings() throws IOException {
		MagicStringBean bean = MagicStringBean.builder()
				.uri(URI.create("https://some.domain.com:9999/path"))
				.url(new URL("https://other.domain.com:1234/path"))
				.uuid(UUID.randomUUID())
				.type(UUID.class)
				.chars("Hello World".toCharArray())
				.bytes("The quick brown fox jumped over the lazy dog".getBytes())
				.build();
		String json = Json.toString(bean,"  ");
		//System.out.println(json);
		MagicStringBean rt = Json.fromString(json, MagicStringBean.class);
		Assert.assertEquals(rt, bean);
		Map raw = (Map) Json.fromString(json);
		Assert.assertEquals(raw.get("chars").getClass(), String.class);
		Assert.assertEquals(raw.get("bytes").getClass(), String.class);
	}
	
	@Value
	@Builder
	public static class MagicStringBean{
		URI uri;
		URL url;
		UUID uuid;
		Class<?> type;
		char[] chars;
		byte[] bytes;
	}
	
	
	@Test
	public void testThrowable() throws IOException {
		ErrorBean bean = ErrorBean.builder()
				.thrown(new RuntimeException("Something happened", new IOException("Soocket timed out")))
				.build();
		String json = Json.toString(bean,"  ");
		//System.out.println(json);
		ErrorBean rt = Json.read(new StringReader(json), ErrorBean.class);
		//System.out.println("After round trip "+ModelJson.toString(rt,"  "));
		Assert.assertTrue(rt.getThrown().getMessage().contains("Something happened"));
		Map raw = (Map) Json.fromString(json);
		Assert.assertEquals(raw.get("thrown").getClass(), String.class);
	}

	@Test
	public void testMagicKeys() throws IOException {
		Map<LocalDate, Integer> data = Map.of(LocalDate.now(), 100);
		String json = Json.toString(data);
		Map<LocalDate, Integer> rt = Json.fromString(json, new StaticType<Map<LocalDate, Integer>>(){});
		Assert.assertEquals(rt, data);
	}

	@Value
	@Builder
	public static class ErrorBean {
		Throwable thrown;
	}
}
