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
package com.bigcloud.djomo.test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.simple.DateTimeParser;
import com.bigcloud.djomo.simple.DateTimePrinter;

public class DateTimeTest {

	@Test
	public void testDatePrinting() {
		ZoneOffset[] offsets = {ZoneOffset.ofHoursMinutesSeconds(-17, 0, 0), ZoneOffset.ofHoursMinutesSeconds(-11, -30, 0), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -30), ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), ZoneOffset.ofHoursMinutesSeconds(1, 5, 5), ZoneOffset.ofHoursMinutesSeconds(13, 30, 0)};
		int[] years = { -975310864, -42975310,-8642975, -723456,-39876,-3450,-423,-63,-1,0,1,72,340,1342,1965,2024,8749,11283, 213094, 8923645, 75310864, 429753186};
		char[] output = new char[64];
		for(ZoneOffset offset: offsets) {
			for(int year: years) {
				for(int month = 1; month<=12;month+=5) {
					for(int day = 1; day <=28; day+=6) {
						for(int hour = 0; hour<24;hour+=6) {
							for(int minute = 0; minute< 60; minute+=12) {
								for(int seconds = 0; seconds < 60; seconds += 15) {
									for(long nanos = 0; nanos < 1000_000_000l; nanos = nanos * 10 + 3) {
										OffsetDateTime dt = OffsetDateTime.of(year, month, month, hour, minute, seconds, (int) nanos, offset);
										String str = dt.toString();
										int pos = new DateTimePrinter.OffsetDateTimePrinter(dt).getChars(output, 0);
										String comp = new String(output, 0, pos);
										OffsetDateTime rt = OffsetDateTime.parse(comp);
										Assert.assertEquals(rt, dt);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Test
	public void testDateParsing() {
		ZoneOffset[] offsets = {ZoneOffset.ofHoursMinutesSeconds(-17, 0, 0), ZoneOffset.ofHoursMinutesSeconds(-11, -30, 0), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -30), ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), ZoneOffset.ofHoursMinutesSeconds(1, 5, 5), ZoneOffset.ofHoursMinutesSeconds(13, 30, 0)};
		int[] years = { -975310864, -42975310,-8642975, -723456,-39876,-3450,-423,-63,-1,0,1,72,340,1342,1965,2024,8749,11283, 213094, 8923645, 75310864, 429753186};
		char[] output = new char[64];
		for(ZoneOffset offset: offsets) {
			for(int year: years) {
				for(int month = 1; month<=12;month+=5) {
					for(int day = 1; day <=28; day+=6) {
						for(int hour = 0; hour<24;hour+=6) {
							for(int minute = 0; minute< 60; minute+=12) {
								for(int seconds = 0; seconds < 60; seconds += 15) {
									for(long nanos = 0; nanos < 1000_000_000l; nanos = nanos * 10 + 3) {
										OffsetDateTime dt = OffsetDateTime.of(year, month, month, hour, minute, seconds, (int) nanos, offset);
										int pos = new DateTimePrinter.OffsetDateTimePrinter(dt).getChars(output, 0);
										String str = new String(output, 0, pos);
										OffsetDateTime rt = parse(str);
										Assert.assertEquals(rt, dt);
									}
									for(long nanos = 1; nanos < 1000_000_000l; nanos = nanos * 10) {
										OffsetDateTime dt = OffsetDateTime.of(year, month, month, hour, minute, seconds, (int) nanos, offset);
										int pos = new DateTimePrinter.OffsetDateTimePrinter(dt).getChars(output, 0);
										String str = new String(output, 0, pos);
										OffsetDateTime rt = parse(str);
										Assert.assertEquals(rt, dt);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Test
	public void testParseMinMaxInstant() {
		String source = Instant.MAX.toString();
		var parser = DateTimeParser.parser(source);
		Instant rt = parser.getInstant();
		Assert.assertEquals(rt, Instant.MAX);
		source = Instant.MIN.toString();
		parser = DateTimeParser.parser(source);
		rt = parser.getInstant();
		Assert.assertEquals(rt, Instant.MIN);

	}
	
	@Test
	public void testOffsetConversion() {
		OffsetDateTime example = OffsetDateTime.of(2025, 12, 25, 12, 55, 55, 0, ZoneOffset.ofHoursMinutes(-8, 0));
		String source = example.toString();
		var parser = DateTimeParser.parser(source);
		Instant rt = parser.getInstant();
		Assert.assertEquals(rt, example.toInstant());
	}
	
	@Test
	public void testAlternativeOffset() {
		var dt = DateTimeParser.parser("2025-12-25T12:55:55+0").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHours(0));
		dt = DateTimeParser.parser("2025-12-25T12:55:55+1").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHours(1));
		dt = DateTimeParser.parser("2025-12-25T12:55:55-10").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHours(-10));
		dt = DateTimeParser.parser("2025-12-25T12:55:55-1030").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutes(-10, -30));
		dt = DateTimeParser.parser("2025-12-25T12:55:55+0815").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutes(8, 15));
		dt = DateTimeParser.parser("2025-12-25T12:55:55-063045").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -45));
		dt = DateTimeParser.parser("2025-12-25T12:55:55+114530").getOffsetDateTime();
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutesSeconds(11, 45, 30));
	}
	
	private OffsetDateTime parse(CharSequence source){
		var parser = DateTimeParser.parser(source);
		return parser.getOffsetDateTime();
	}
}
