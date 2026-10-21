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
import static org.testng.Assert.assertNull;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Model;

public class ConversionTest {
	Models Models = new Models();

	@Test
	public void testConvertMapToBean() {
		Map source = Map.of("name", "foo", "count", "123", "enabled", "true");
		ImmutableModel dest = Models.get(ImmutableModel.class).convert(source);
		assertEquals(dest.getName(), "foo");
		assertEquals(dest.getCount(), 123);
		assertEquals(dest.isEnabled(), true);
	}

	@Test
	public void testConvertBeanToMap() {
		ImmutableModel source = ImmutableModel.builder().name("foo").count(123).enabled(true).build();
		Map dest = Models.get(Map.class).convert(source);
		assertEquals(dest.get("name"), "foo");
		assertEquals(dest.get("count"), 123);
		assertEquals(dest.get("enabled"), true);
	}

	@Test
	public void testConvertListToArray() {
		List<String> test = List.of("a", "b", "c");
		String[] strings = Models.get(String[].class).convert(test);
		Assert.assertEquals(String.join("", strings), "abc");
	}

	@Test
	public void testConvertArrayToList() {
		String[] test = new String[] { "a", "b", "c" };
		List<String> strings = (List<String>) Models.get(new com.bigcloud.djomo.StaticType<List<String>>() {
		}).convert(test);
		Assert.assertEquals(strings.stream().collect(Collectors.joining()), "abc");
	}

	enum SampleEnum {
		VALUE_ONE, VALUE_TWO
	}
	/**
	 * Parameterized test that exercises convert() for all major types with: - null
	 * values - matching instances - string representations in appropriate format
	 */
	@DataProvider(name = "allTypesProvider")
	public Object[][] provideAllTypesConversionData() {
		return new Object[][] {
				// Primitives and Wrappers
				{ Integer.class, null, 42, "42" },
				{ int.class, null, 42, "42" },
				{ Long.class, null, 100L, "100" },
				{ long.class, null, 100L, "100" },
				{ Double.class, null, 3.14, "3.14" },
				{ double.class, null, 3.14, "3.14" },
				{ Float.class, null, 2.71f, "2.71" },
				{ float.class, null, 2.71f, "2.71" },
				{ Byte.class, null, (byte) 15, "15" },
				{ byte.class, null, (byte) 15, "15" },
				{ Short.class, null, (short) 500, "500" },
				{ short.class, null, (short) 500, "500" },
				{ Boolean.class, null, true, "true" },
				{ boolean.class, null, true, "true" },
				{ Character.class, null, 'X', "X" },
				{ char.class, null, 'X', "X" },

				// Number types
				{ BigDecimal.class, null, new BigDecimal("123.45"), "123.45" },
				{ BigInteger.class, null, new BigInteger("999"), "999" },
				{ Number.class, null, 42.0, "42.0" },

				// String
				{ String.class, null, "hello", "world" },

				// UUID
				{ UUID.class, null, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
						"550e8400-e29b-41d4-a716-446655440000" },

				// Date/Time types
				{ LocalDate.class, null, LocalDate.of(2023, 6, 15), "2023-06-15" },
				{ LocalTime.class, null, LocalTime.of(14, 30, 45), "14:30:45" },
				{ LocalDateTime.class, null, LocalDateTime.of(2023, 6, 15, 14, 30, 45), "2023-06-15T14:30:45" },
				{ Instant.class, null, Instant.parse("2023-06-15T14:30:45Z"), "2023-06-15T14:30:45Z" },
				{ ZonedDateTime.class, null, ZonedDateTime.parse("2023-06-15T14:30:45+02:00"),
						"2023-06-15T14:30:45+02:00" },
				{ OffsetDateTime.class, null, OffsetDateTime.parse("2023-06-15T14:30:45+02:00"),
						"2023-06-15T14:30:45+02:00" },
				{ OffsetTime.class, null, OffsetTime.parse("14:30:45+02:00"), "14:30:45+02:00" },
				{ Year.class, null, Year.of(2023), "2023" },
				{ YearMonth.class, null, YearMonth.of(2023, 6), "2023-06" },
				{ MonthDay.class, null, MonthDay.of(6, 15), "--06-15" },
				{ ZoneOffset.class, null, ZoneOffset.ofHours(5), "+05:00" },
				{ ZoneId.class, null, ZoneId.of("America/New_York"), "America/New_York" },
				{ Duration.class, null, Duration.ofHours(2), "PT2H" },
				{ Period.class, null, Period.of(1, 2, 3), "P1Y2M3D" },

				// URL and URI types
				{ URI.class, null, URI.create("https://example.com"), "https://example.com" },

				// Arrays
				{ String[].class, null, new String[] { "a", "b", "c" }, null },
				{ int[].class, null, new int[] { 1, 2, 3 }, null },
				{ byte[].class, null, new byte[] { 1, 2, 3 }, "AQID" },
				{ char[].class, null, new char[] { 'A', 'B', 'C' }, "ABC" },

				// Map and List
				{ Map.class, null, Map.of("key", "value"), null },
				{ List.class, null, List.of(1, 2, 3), null },
				
				// Enum
				{ SampleEnum.class, null, SampleEnum.VALUE_ONE, "VALUE_ONE" }
		};
	}

	@Test(dataProvider = "allTypesProvider")
	public void testConvertAllTypes(Type type, Object unused, Object matchingInstance, Object stringInput) {
		Model<?> model = Models.get(type);
		String testName = type.getTypeName() + " with inputs: null, " + matchingInstance + ", " + stringInput;

		// Test null conversion (skip for types that have default conversions)
		if (isPrimitiveType(type)) {
			// Primitives cannot be null, skip this case
		} else if (!Boolean.class.equals(type)) {
			// Boolean has special handling and converts null to false, so skip it
			Object nullResult = model.convert(null);
			assertNull(nullResult, "Null conversion should return null for " + testName);
		}

		// Test matching instance conversion
		Object matchingResult = model.convert(matchingInstance);
		if (!isPrimitiveType(type) && matchingInstance != null) {
			assertEquals(matchingResult, matchingInstance,
					"Converting matching instance should return equivalent value for " + testName);
		}

		// Test string conversion (where applicable)
		if (stringInput != null && !String.class.equals(type)) {
			// String type just returns the same string, so comparison is different
			Object stringResult = model.convert(stringInput.toString());
			assertEquals(stringResult, matchingInstance,
					"Converting string should parse to matching instance for " + testName);
		}
	}

	private boolean isPrimitiveType(Type type) {
		if (!(type instanceof Class)) {
			return false;
		}
		Class<?> clazz = (Class<?>) type;
		return clazz.isPrimitive();
	}
	//
	// @Test(expectedExceptions = RuntimeException.class)
	// public void testStrictConvertFail() {
	// var model = ((ObjectModel<ImmutableModel>)Models.get(ImmutableModel.class));
	// ObjectMaker<ImmutableModel> maker =model.maker();
	// maker.setField(model.getField("name"), "foo");
	// maker.setField(model.getField("count"), "123");
	// maker.setField(model.getField("enabled"), "true");
	// }
}