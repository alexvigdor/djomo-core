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
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.filter.parsers.FieldParser;
import com.bigcloud.djomo.filter.parsers.LimitParser;
import com.bigcloud.djomo.filter.visitors.FieldVisitor;
import com.bigcloud.djomo.filter.visitors.LimitVisitor;

public class FieldFilterTest {
	public static record Feed(List headlines, List links) {
	}

	Json json = new Json();

	@Test
	public void testFieldFilters() throws IOException {
		Feed feed = new Feed(List.of("one", Map.of("hooples", List.of("two", "three", "four")), "five", "six"),
				List.of("a", "b", "c", "d"));
		var visitorFilter = new FieldVisitor<Feed>("headlines", new LimitVisitor(2)) {};
		Assert.assertEquals(visitorFilter.getType(), Feed.class);
		String str = json.toString(feed, visitorFilter);
		Assert.assertEquals(str,
				"{\"headlines\":[\"one\",{\"hooples\":[\"two\",\"three\",\"four\"]}],\"links\":[\"a\",\"b\",\"c\",\"d\"]}");
		var parserFilter = new FieldParser<Feed>("links", new LimitParser(3)) {};
		Assert.assertEquals(parserFilter.getType(), Feed.class);
		Feed feed2 = json.fromString(str, Feed.class, parserFilter);
		str = json.toString(feed2);
		Assert.assertEquals(str,
				"{\"headlines\":[\"one\",{\"hooples\":[\"two\",\"three\",\"four\"]}],\"links\":[\"a\",\"b\",\"c\"]}");
	}

	@Test
	public void testFieldFunctions() throws IOException {
		Feed feed = new Feed(List.of("one", "two", "three", "four", "five", "six"), List.of("a", "b", "c", "d"));
		String str = json.toString(feed,
				new FieldVisitor<Feed>("headlines", Filters.visitList((list, model, visitor) -> visitor.visitString(String.join(",", (List<String>) list)))){});
		Assert.assertEquals(str,
				"{\"headlines\":\"one,two,three,four,five,six\",\"links\":[\"a\",\"b\",\"c\",\"d\"]}");
		Feed feed2 = json.fromString(str, Feed.class,
				new FieldParser<Feed>("headlines", Filters.parseList((model, parser) -> Arrays.asList(parser.parseString().toString().split(",")))) {});
		str = json.toString(feed2);
		Assert.assertEquals(str,
				"{\"headlines\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],\"links\":[\"a\",\"b\",\"c\",\"d\"]}");
	}
}
