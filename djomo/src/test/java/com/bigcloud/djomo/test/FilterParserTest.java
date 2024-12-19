/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.parsers.ModelParser;
import com.bigcloud.djomo.api.parsers.ObjectParser;

public class FilterParserTest {
	Json json = new Json();

	@Test
	public void testBooleanParserFilter() throws IOException {
		var filter = Filters.parseBoolean(p -> p.parseInt() == 1);
		boolean result = json.fromString("1", Boolean.class, filter);
		assertEquals(result, true);
		var result2 = json.fromString("[1]", new StaticType<List<Boolean>>() {}, filter);
		assertEquals(result2.get(0), true);
		var result3 = json.fromString("{\"a\":0}", new StaticType<Map<String, Boolean>>() {}, filter);
		assertEquals(result3.get("a"), false);
	}

	@Test
	public void testDoubleParserFilter() throws IOException {
		var filter = Filters.parseDouble(p -> p.parseInt() / 100.0);
		var result = json.fromString("10", Double.class, filter);
		assertEquals(result, 0.1);
		var result2 = json.fromString("[25]", new StaticType<List<Double>>() {}, filter);
		assertEquals(result2.get(0), 0.25);
		var result3 = json.fromString("{\"a\":110}", new StaticType<Map<String, Double>>() {}, filter);
		assertEquals(result3.get("a"), 1.1);
	}

	@Test
	public void testFloatParserFilter() throws IOException {
		var filter = Filters.parseFloat(p -> p.parseInt() / 100.0f);
		var result = json.fromString("10", Float.class, filter);
		assertEquals(result, 0.1f);
		var result2 = json.fromString("[25]", new StaticType<List<Float>>() {}, filter);
		assertEquals(result2.get(0), 0.25f);
		var result3 = json.fromString("{\"a\":110}", new StaticType<Map<String, Float>>() {}, filter);
		assertEquals(result3.get("a"), 1.1f);
	}

	@Test
	public void testIntParserFilter() throws IOException {
		var filter = Filters.parseInt(p -> (int) Math.round(p.parseDouble() * 100));
		var result = json.fromString("0.1", Integer.class, filter);
		assertEquals(result, 10);
		var result2 = json.fromString("[0.25]", new StaticType<List<Integer>>() {}, filter);
		assertEquals(result2.get(0), 25);
		var result3 = json.fromString("{\"a\":1.1}", new StaticType<Map<String, Integer>>() {}, filter);
		assertEquals(result3.get("a"), 110);
	}

	@Test
	public void testLongParserFilter() throws IOException {
		var filter = Filters.parseLong(p -> (int) Math.round(p.parseDouble() * 100));
		var result = json.fromString("0.1", Long.class, filter);
		assertEquals(result, 10);
		var result2 = json.fromString("[0.25]", new StaticType<List<Long>>() {}, filter);
		assertEquals(result2.get(0), 25);
		var result3 = json.fromString("{\"a\":1.1}", new StaticType<Map<String, Long>>() {}, filter);
		assertEquals(result3.get("a"), 110);
	}

	@Test
	public void testStringParserFilter() throws IOException {
		var filter = Filters.parseString((p -> p.parseString().toString().repeat(2)));
		var result = json.fromString("\"hi\"", filter);
		assertEquals(result, "hihi");
		var result2 = json.fromString("[\"hi\"]", filter);
		assertEquals(result2, List.of("hihi"));
		var result3 = json.fromString("{\"a\":\"hi\"}", filter);
		assertEquals(result3, Map.of("a", "hihi"));
	}

	public static class ListWrapper {
		public List values;
	}

	@Test
	public void testListParserFilter() throws IOException {
		var localContext = new ThreadLocal<Boolean>() {
			public Boolean initialValue() {
				return false;
			}
		};
		ObjectModel<ListWrapper> listWrapperModel = json.models().get(ListWrapper.class);
		var filter = Filters.parseList((m, p) -> {
			if (!localContext.get()) {
				localContext.set(true);
				ListWrapper wrapper = (ListWrapper) p.parseObject(listWrapperModel);
				localContext.set(false);
				return wrapper.values;
			} else {
				return p.parseList(m);
			}
		});
		var result = json.fromString("{\"values\":[1,2,3]}", List.class, filter);
		assertEquals(result, List.of(1, 2, 3));
		var result2 = json.fromString("{\"a\":{\"values\":[1,2,3]}}", new StaticType<Map<String, List<Integer>>>() {}, filter);
		assertEquals(result2, Map.of("a", List.of(1, 2, 3)));
	}

	@Test
	public void testModelParserFilter() throws IOException {
		var filter = Filters.parseModel((m, p) -> {
			if (UUID.class.isAssignableFrom(m.getType())) {
				List l = p.models().listModel.parse(p);
				return new UUID((int) l.get(0), (int) l.get(1));
			}
			return p.parse(m);
		});
		var uuid = new UUID(123456789, 987654321);
		// top-level parse model filtering doesn't work since we removed the filter
		// method ...
		// var result = json.fromString("[123456789,987654321]", UUID.class, filter);
		// assertEquals(result, uuid);
		var result2 = json.fromString("[[123456789,987654321]]", new StaticType<List<UUID>>() {}, filter);
		assertEquals(result2, List.of(uuid));
		var result3 = json.fromString("{\"a\":[123456789,987654321]}", new StaticType<Map<String, UUID>>() {}, filter);
		assertEquals(result3, Map.of("a", uuid));
	}

	@Test
	public void testRuntimeTypedModelParserFilter() throws IOException {
		var filter = Filters.parseModel(UUID.class,
				(m, p) -> {
					List l = p.models().listModel.parse(p);
					return new UUID((int) l.get(0), (int) l.get(1));
				});
		var uuid = new UUID(123456789, 987654321);
		// var result = json.fromString("[123456789,987654321]", UUID.class, filter);
		// assertEquals(result, uuid);
		var result2 = json.fromString("[[123456789,987654321]]", new StaticType<List<UUID>>() {}, filter);
		assertEquals(result2, List.of(uuid));
		var result3 = json.fromString("{\"a\":[123456789,987654321]}", new StaticType<Map<String, UUID>>() {}, filter);
		assertEquals(result3, Map.of("a", uuid));
	}

	@Test
	public void testCompilerTypedModelParserFilter() throws IOException {
		class MyParser implements ModelParser<UUID> {

			@Override
			public UUID parse(Model<UUID> model, Parser parser) {
				List l = parser.models().listModel.parse(parser);
				return new UUID((int) l.get(0), (int) l.get(1));
			}

		}
		var filter = new MyParser();
		var uuid = new UUID(123456789, 987654321);
		// var result = json.fromString("[123456789,987654321]", UUID.class, filter);
		// assertEquals(result, uuid);
		var result2 = json.fromString("[[123456789,987654321]]", new StaticType<List<UUID>>() {}, filter);
		assertEquals(result2, List.of(uuid));
		var result3 = json.fromString("{\"a\":[123456789,987654321]}", new StaticType<Map<String, UUID>>() {}, filter);
		assertEquals(result3, Map.of("a", uuid));
	}

	public static record Identifier(long mostSignificantBits, long leastSignificantBits) {

	}

	@Test
	public void testObjectParserFilter() throws IOException {
		var filter = Filters.parseObject((m, p) -> {
			if (Identifier.class.isAssignableFrom(m.getType())) {
				List l = p.models().listModel.parse(p);
				return new Identifier((int) l.get(0), (int) l.get(1));
			}
			return p.parseObject(m);
		});
		var identifier = new Identifier(123456789, 987654321);
		var result2 = json.fromString("[[123456789,987654321]]", new StaticType<List<Identifier>>() {}, filter);
		assertEquals(result2, List.of(identifier));
		var result3 = json.fromString("{\"a\":[123456789,987654321]}", new StaticType<Map<String, Identifier>>() {}, filter);
		assertEquals(result3, Map.of("a", identifier));
	}

	@Test
	public void testRuntimeTypedObjectVisitorFilter() throws IOException {
		var filter = Filters.parseObject(Identifier.class,
				(m, p) -> {
					List l = p.models().listModel.parse(p);
					return new Identifier((int) l.get(0), (int) l.get(1));
				});
		var identifier = new Identifier(123456789, 987654321);
		var result2 = json.fromString("[[123456789,987654321]]", new StaticType<List<Identifier>>() {}, filter);
		assertEquals(result2, List.of(identifier));
		var result3 = json.fromString("{\"a\":[123456789,987654321]}", new StaticType<Map<String, Identifier>>() {}, filter);
		assertEquals(result3, Map.of("a", identifier));
	}

	@Test
	public void testCompilerTypedObjectVisitorFilter() throws IOException {
		class MyParser implements ObjectParser<Identifier> {

			@Override
			public Identifier parseObject(ObjectModel<Identifier> model, Parser parser) {
				List l = parser.models().listModel.parse(parser);
				return new Identifier((int) l.get(0), (int) l.get(1));
			}

		}
		var filter = new MyParser();
		var identifier = new Identifier(123456789, 987654321);
		var result2 = json.fromString("[[123456789,987654321]]", new StaticType<List<Identifier>>() {}, filter);
		assertEquals(result2, List.of(identifier));
		var result3 = json.fromString("{\"a\":[123456789,987654321]}", new StaticType<Map<String, Identifier>>() {}, filter);
		assertEquals(result3, Map.of("a", identifier));
	}
}