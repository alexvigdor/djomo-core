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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.filter.FieldParser;
import com.bigcloud.djomo.filter.FieldParserFunction;
import com.bigcloud.djomo.filter.FieldVisitor;
import com.bigcloud.djomo.filter.FieldVisitorFunction;
import com.bigcloud.djomo.filter.LimitParser;
import com.bigcloud.djomo.filter.LimitVisitor;
import com.bigcloud.djomo.filter.TypeVisitor;

public class FieldFilterTest {
	public static record Feed(List headlines, List links) {
	}

	Json json = new Json();

	@Test
	public void testFieldFilters() throws IOException {
		Feed feed = new Feed(List.of("one", Map.of("headlines", List.of("two", "three", "four")), "five", "six"),
				List.of("a", "b", "c", "d"));
		String str = json.toString(feed, new FieldVisitor<Feed>("headlines", new LimitVisitor(2)) {});
		Assert.assertEquals(str,
				"{\"headlines\":[\"one\",{\"headlines\":[\"two\",\"three\",\"four\"]}],\"links\":[\"a\",\"b\",\"c\",\"d\"]}");
		Feed feed2 = json.fromString(str, Feed.class, new FieldParser<Feed>("links", new LimitParser(3)) {});
		str = json.toString(feed2);
		Assert.assertEquals(str,
				"{\"headlines\":[\"one\",{\"headlines\":[\"two\",\"three\",\"four\"]}],\"links\":[\"a\",\"b\",\"c\"]}");
	}

	@Test
	public void testFieldFunctions() throws IOException {
		Feed feed = new Feed(List.of("one", "two", "three", "four", "five", "six"), List.of("a", "b", "c", "d"));
		String str = json.toString(feed,
				new FieldVisitorFunction<Feed, List<String>>("headlines", hs -> String.join(",", hs)) {});
		Assert.assertEquals(str,
				"{\"headlines\":\"one,two,three,four,five,six\",\"links\":[\"a\",\"b\",\"c\",\"d\"]}");
		Feed feed2 = json.fromString(str, Feed.class,
				new FieldParserFunction<Feed, String, List<String>>("headlines", s -> Arrays.asList(s.split(","))) {});
		str = json.toString(feed2);
		Assert.assertEquals(str,
				"{\"headlines\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],\"links\":[\"a\",\"b\",\"c\",\"d\"]}");
	}
}
