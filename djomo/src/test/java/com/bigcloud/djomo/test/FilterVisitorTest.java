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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.visitors.ModelVisitor;
import com.bigcloud.djomo.api.visitors.ObjectVisitor;

public class FilterVisitorTest {
	Json json = new Json();

	@Test
	public void testBooleanVisitorFilter() {
		var filter = Filters.visitBoolean((b, v) -> v.visitInt(b ? 1 : 0));
		var result = json.toString(true, filter);
		assertEquals(result, "1");
		result = json.toString(List.of(true), filter);
		assertEquals(result, "[1]");
		result = json.toString(Map.of("a", false), filter);
		assertEquals(result, "{\"a\":0}");
	}

	@Test
	public void testDoubleVisitorFilter() {
		var filter = Filters.visitDouble((d, v) -> v.visitInt((int) (d * 100)));
		var result = json.toString(0.1, filter);
		assertEquals(result, "10");
		result = json.toString(List.of(0.25), filter);
		assertEquals(result, "[25]");
		result = json.toString(Map.of("a", 1.1), filter);
		assertEquals(result, "{\"a\":110}");
	}

	@Test
	public void testFloatVisitorFilter() {
		var filter = Filters.visitFloat((f, v) -> v.visitInt((int) (f * 100)));
		var result = json.toString(0.1f, filter);
		assertEquals(result, "10");
		result = json.toString(List.of(0.25f), filter);
		assertEquals(result, "[25]");
		result = json.toString(Map.of("a", 1.1f), filter);
		assertEquals(result, "{\"a\":110}");
	}

	@Test
	public void testIntVisitorFilter() {
		var filter = Filters.visitInt((i, v) -> v.visitDouble(i / 100.0));
		var result = json.toString(10, filter);
		assertEquals(result, "0.1");
		result = json.toString(List.of(25), filter);
		assertEquals(result, "[0.25]");
		result = json.toString(Map.of("a", 110), filter);
		assertEquals(result, "{\"a\":1.1}");
	}

	@Test
	public void testListVisitorFilter() {
		var localContext = new ThreadLocal<Boolean>() {
			public Boolean initialValue() {
				return false;
			}
		};
		var filter = Filters.visitList((l, m, v) -> {
			if (!localContext.get()) {
				localContext.set(true);
				v.visit(Map.of("values", l));
				localContext.set(false);
			} else {
				v.visitList(l, m);
			}
		});
		var result = json.toString(List.of(1, 2, 3), filter);
		assertEquals(result, "{\"values\":[1,2,3]}");
		result = json.toString(Map.of("a", List.of(1, 2, 3)), filter);
		assertEquals(result, "{\"a\":{\"values\":[1,2,3]}}");
	}

	@Test
	public void testLongVisitorFilter() {
		var filter = Filters.visitLong((l, v) -> v.visitDouble(l / 100.0));
		var result = json.toString(10l, filter);
		assertEquals(result, "0.1");
		result = json.toString(List.of(25l), filter);
		assertEquals(result, "[0.25]");
		result = json.toString(Map.of("a", 110l), filter);
		assertEquals(result, "{\"a\":1.1}");
	}

	@Test
	public void testModelVisitorFilter() {
		var filter = Filters.visitModel((o, m, v) -> {
			if (o instanceof UUID u) {
				v.visit(Stream.of(u.getMostSignificantBits(), u.getLeastSignificantBits()));
			} else {
				v.visit(o, m);
			}
		});
		var uuid = new UUID(123456789, 987654321);
		var result = json.toString(uuid, filter);
		assertEquals(result, "[123456789,987654321]");
		var list = List.of("a", 1, uuid);
		result = json.toString(list, filter);
		assertEquals(result, "[\"a\",1,[123456789,987654321]]");
		var map = Map.of("a", uuid);
		result = json.toString(map, filter);
		assertEquals(result, "{\"a\":[123456789,987654321]}");
	}

	@Test
	public void testRuntimeTypedModelVisitorFilter() {
		var filter = Filters.visitModel(UUID.class,
				(u, m, v) -> v.visit(Stream.of(u.getMostSignificantBits(), u.getLeastSignificantBits())));
		var uuid = new UUID(123456789, 987654321);
		var result = json.toString(uuid, filter);
		assertEquals(result, "[123456789,987654321]");
		var list = List.of("a", 1, uuid);
		result = json.toString(list, filter);
		assertEquals(result, "[\"a\",1,[123456789,987654321]]");
		var map = Map.of("a", uuid);
		result = json.toString(map, filter);
		assertEquals(result, "{\"a\":[123456789,987654321]}");
	}

	@Test
	public void testCompilerTypedModelVisitorFilter() {
		class MyVisitor implements ModelVisitor<UUID> {

			@Override
			public void visitModel(UUID u, Model<UUID> m, Visitor v) {
				v.visit(Stream.of(u.getMostSignificantBits(), u.getLeastSignificantBits()));
			}

		}
		var filter = new MyVisitor();
		var uuid = new UUID(123456789, 987654321);
		var result = json.toString(uuid, filter);
		assertEquals(result, "[123456789,987654321]");
		var list = List.of("a", 1, uuid);
		result = json.toString(list, filter);
		assertEquals(result, "[\"a\",1,[123456789,987654321]]");
		var map = Map.of("a", uuid);
		result = json.toString(map, filter);
		assertEquals(result, "{\"a\":[123456789,987654321]}");
	}

	public static record Identifier(long mostSignificantBits, long leastSignificantBits) {

	}

	@Test
	public void testObjectVisitorFilter() {
		var filter = Filters.visitObject((o, m, v) -> {
			if (o instanceof Identifier u) {
				v.visit(Stream.of(u.mostSignificantBits(), u.leastSignificantBits()));
			} else {
				v.visitObject(o, m);
			}
		});
		var identifier = new Identifier(123456789, 987654321);
		var result = json.toString(identifier, filter);
		assertEquals(result, "[123456789,987654321]");
		var list = List.of("a", 1, identifier);
		result = json.toString(list, filter);
		assertEquals(result, "[\"a\",1,[123456789,987654321]]");
		var map = Map.of("a", identifier);
		result = json.toString(map, filter);
		assertEquals(result, "{\"a\":[123456789,987654321]}");
	}

	@Test
	public void testRuntimeTypedObjectVisitorFilter() {
		var filter = Filters.visitObject(Identifier.class,
				(u, m, v) -> v.visit(Stream.of(u.mostSignificantBits(), u.leastSignificantBits())));
		var identifier = new Identifier(123456789, 987654321);
		var result = json.toString(identifier, filter);
		assertEquals(result, "[123456789,987654321]");
		var list = List.of("a", 1, identifier);
		result = json.toString(list, filter);
		assertEquals(result, "[\"a\",1,[123456789,987654321]]");
		var map = Map.of("a", identifier);
		result = json.toString(map, filter);
		assertEquals(result, "{\"a\":[123456789,987654321]}");
	}

	@Test
	public void testCompilerTypedObjectVisitorFilter() {
		class MyVisitor implements ObjectVisitor<Identifier> {

			@Override
			public void visitObject(Identifier u, ObjectModel<Identifier> model, Visitor v) {
				v.visit(Stream.of(u.mostSignificantBits(), u.leastSignificantBits()));
			}

		}
		var filter = new MyVisitor();
		var identifier = new Identifier(123456789, 987654321);
		var result = json.toString(identifier, filter);
		assertEquals(result, "[123456789,987654321]");
		var list = List.of("a", 1, identifier);
		result = json.toString(list, filter);
		assertEquals(result, "[\"a\",1,[123456789,987654321]]");
		var map = Map.of("a", identifier);
		result = json.toString(map, filter);
		assertEquals(result, "{\"a\":[123456789,987654321]}");
	}

	@Test
	public void testStringVisitorFilter() {
		var filter = Filters.visitString((s, v) -> v.visitString(s.toString().repeat(2)));
		var result = json.toString("hi", filter);
		assertEquals(result, "\"hihi\"");
		result = json.toString(List.of("hi"), filter);
		assertEquals(result, "[\"hihi\"]");
		result = json.toString(Map.of("a", "hi"), filter);
		assertEquals(result, "{\"a\":\"hihi\"}");
	}

}
