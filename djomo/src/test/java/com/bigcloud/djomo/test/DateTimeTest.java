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

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.Ignore;

import com.bigcloud.djomo.Json;

public class DateTimeTest {
	Json json = new Json();

	@Test
	public void testDatePrinting() {
		ZoneOffset[] offsets = { ZoneOffset.ofHoursMinutesSeconds(-17, 0, 0),
				ZoneOffset.ofHoursMinutesSeconds(-11, -30, 0), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -30),
				ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), ZoneOffset.ofHoursMinutesSeconds(1, 5, 5),
				ZoneOffset.ofHoursMinutesSeconds(13, 30, 0) };
		int[] years = { -975310864, -42975310, -8642975, -723456, -39876, -3450, -423, -63, -1, 0, 1, 72, 340, 1342,
				1965, 2024, 8749, 11283, 213094, 8923645, 75310864, 429753186 };
		char[] output = new char[64];
		for (ZoneOffset offset : offsets) {
			for (int year : years) {
				for (int month = 1; month <= 12; month += 5) {
					for (int day = 1; day <= 28; day += 6) {
						for (int hour = 0; hour < 24; hour += 6) {
							for (int minute = 0; minute < 60; minute += 12) {
								for (int seconds = 0; seconds < 60; seconds += 15) {
									for (long nanos = 0; nanos < 1000_000_000l; nanos = nanos * 10 + 3) {
										OffsetDateTime dt = OffsetDateTime.of(year, month, month, hour, minute, seconds,
												(int) nanos, offset);
										String str = dt.toString();
										// int pos = new DateTimePrinter.OffsetDateTimePrinter(dt).getChars(output, 0);
										String comp = json.toString(dt); // new String(output, 0, pos);
										comp = comp.substring(1, comp.length() - 1);
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
	public void testDateParsing() throws IOException {
		ZoneOffset[] offsets = { ZoneOffset.ofHoursMinutesSeconds(-17, 0, 0),
				ZoneOffset.ofHoursMinutesSeconds(-11, -30, 0), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -30),
				ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), ZoneOffset.ofHoursMinutesSeconds(1, 5, 5),
				ZoneOffset.ofHoursMinutesSeconds(13, 30, 0) };
		int[] years = { -975310864, -42975310, -8642975, -723456, -39876, -3450, -423, -63, -1, 0, 1, 72, 340, 1342,
				1965, 2024, 8749, 11283, 213094, 8923645, 75310864, 429753186 };
		for (ZoneOffset offset : offsets) {
			for (int year : years) {
				for (int month = 1; month <= 12; month += 5) {
					for (int day = 1; day <= 28; day += 6) {
						for (int hour = 0; hour < 24; hour += 6) {
							for (int minute = 0; minute < 60; minute += 12) {
								for (int seconds = 0; seconds < 60; seconds += 15) {
									for (long nanos = 0; nanos < 1000_000_000l; nanos = nanos * 10 + 3) {
										OffsetDateTime dt = OffsetDateTime.of(year, month, month, hour, minute, seconds,
												(int) nanos, offset);
										String str = json.toString(dt);
										str = str.substring(1, str.length() - 1);
										OffsetDateTime rt = parse(str);
										Assert.assertEquals(rt, dt);
									}
									for (long nanos = 1; nanos < 1000_000_000l; nanos = nanos * 10) {
										OffsetDateTime dt = OffsetDateTime.of(year, month, month, hour, minute, seconds,
												(int) nanos, offset);
										String str = json.toString(dt);
										str = str.substring(1, str.length() - 1);
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
	@Ignore
	// a longer running test to allow performance evaluations
	public void timedParsingTestNormallyDisabled() throws IOException {
		ZoneOffset[] offsets = { ZoneOffset.ofHoursMinutesSeconds(-17, 0, 0),
				ZoneOffset.ofHoursMinutesSeconds(-11, -30, 0), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -30),
				ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), ZoneOffset.ofHoursMinutesSeconds(1, 5, 5),
				ZoneOffset.ofHoursMinutesSeconds(13, 30, 0) };
		int[] years = { -975310864, -42975310, -8642975, -723456, -39876, -3450, -423, -63, -1, 0, 1, 72, 340, 1342,
				1965, 2024, 8749, 11283, 213094, 8923645, 75310864, 429753186 };
		ArrayList<String> dataSet = new ArrayList<>();
		for (ZoneOffset offset : offsets) {
			for (int year : years) {
				String prefix = year < 10000 ? "" : "+";
				dataSet.add("\"" + prefix + String.valueOf(year) + "\"");
				for (int month = 1; month <= 12; month += 5) {
					dataSet.add("\"" + prefix + year + "-" + month + "\"");
					for (int day = 1; day <= 28; day += 6) {
						dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "\"");
						for (int hour = 0; hour < 24; hour += 6) {
							for (int minute = 0; minute < 60; minute += 12) {
								dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":" + minute
										+ "\"");
								for (int seconds = 0; seconds < 60; seconds += 15) {
									dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":"
											+ minute + ":" + seconds + "\"");
									dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":"
											+ minute + ":" + seconds + "Z\"");
									for (long nanos = 0; nanos < 1000_000_000l; nanos = nanos * 10 + 3) {
										dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":"
												+ minute + ":" + seconds + "." + nanos + "\"");
										dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":"
												+ minute + ":" + seconds + "." + nanos + "Z\"");
										dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":"
												+ minute + ":" + seconds + "." + nanos + offset.toString() + "\"");
										dataSet.add("\"" + prefix + year + "-" + month + "-" + day + "T" + hour + ":"
												+ minute + ":" + seconds + "." + nanos + offset.toString()
												+ "[Europe/Paris]\"");
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println("Starting test with dataset " + dataSet.size());
		Json json = new Json();

		TemporalAccessor time;
		String[] data = dataSet.toArray(String[]::new);
		dataSet.clear();

		int warmupIters = 4;
		for (int i = 0; i < warmupIters; i++) {
			for (String str : data) {
				time = json.fromString(str, TemporalAccessor.class);
			}
		}

		int iters = 32;
		long[] results = new long[iters];

		for (int i = 0; i < iters; i++) {
			long time1 = System.nanoTime();
			for (String str : data) {
				time = json.fromString(str, TemporalAccessor.class);
			}
			long time2 = System.nanoTime();
			results[i] = (time2 - time1) / 1_000_000;
		}
		Arrays.sort(results);
		System.out.println("Completed fastest in " + results[0] + " median " + results[iters / 2]);
	}

	@Test
	public void testParseMinMaxInstant() throws IOException {
		String source = Instant.MAX.toString();
		Instant rt = json.fromString("\"" + source + "\"", Instant.class);
		Assert.assertEquals(rt, Instant.MAX);
		source = Instant.MIN.toString();
		rt = json.fromString("\"" + source + "\"", Instant.class);
		Assert.assertEquals(rt, Instant.MIN);

	}

	@Test
	public void testOffsetConversion() throws IOException {
		OffsetDateTime example = OffsetDateTime.of(2025, 12, 25, 12, 55, 55, 0, ZoneOffset.ofHoursMinutes(-8, 0));
		String source = example.toString();
		Instant rt = json.fromString("\"" + source + "\"", Instant.class);
		Assert.assertEquals(rt, example.toInstant());
	}

	@Test
	public void testAlternativeOffset() throws IOException {
		var dt = parse("2025-12-25T12:55:55+0");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHours(0));
		dt = parse("2025-12-25T12:55:55+1");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHours(1));
		dt = parse("2025-12-25T12:55:55-10");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHours(-10));
		dt = parse("2025-12-25T12:55:55-1030");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutes(-10, -30));
		dt = parse("2025-12-25T12:55:55+0815");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutes(8, 15));
		dt = parse("2025-12-25T12:55:55-063045");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutesSeconds(-6, -30, -45));
		dt = parse("2025-12-25T12:55:55+114530");
		Assert.assertEquals(dt.getOffset(), ZoneOffset.ofHoursMinutesSeconds(11, 45, 30));
	}

	private OffsetDateTime parse(CharSequence source) throws IOException {
		return json.fromString("\"" + source + "\"", OffsetDateTime.class);
	}
}
