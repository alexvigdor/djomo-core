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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.filter.ExcludeVisitor;
import com.bigcloud.djomo.filter.FieldParserFunction;
import com.bigcloud.djomo.filter.FieldVisitorFunction;
import com.bigcloud.djomo.filter.FilterVisitor;
import com.bigcloud.djomo.filter.IncludeParser;
import com.bigcloud.djomo.filter.IncludeVisitor;
import com.bigcloud.djomo.filter.LimitParser;
import com.bigcloud.djomo.filter.LimitVisitor;
import com.bigcloud.djomo.filter.MultiFilterParser;
import com.bigcloud.djomo.filter.MultiFilterVisitor;
import com.bigcloud.djomo.filter.OmitNullParser;
import com.bigcloud.djomo.filter.OmitNullVisitor;
import com.bigcloud.djomo.filter.PathParser;
import com.bigcloud.djomo.filter.PathVisitor;
import com.bigcloud.djomo.filter.RenameParser;
import com.bigcloud.djomo.filter.RenameVisitor;
import com.bigcloud.djomo.filter.TypeParser;
import com.bigcloud.djomo.filter.TypeParserFunction;
import com.bigcloud.djomo.filter.TypeVisitor;
import com.bigcloud.djomo.filter.TypeVisitorFunction;

import lombok.Builder;
import lombok.Value;

public class TransformTest {
	Json Json = new Json();
	@Test
	public void testTransformPrecedence() {
		Map sample = map( 
						"foo", "bar",
						"a", map("foo","car", "b", Map.of("foo", "jar")),
						"c", map("foo","far","d",Map.of("foo", "par")));
		PathVisitor filter = PathVisitor.builder()
				.filter("**.foo", new TypeVisitorFunction<String>(String::toUpperCase) {})
				.filter("a.**.foo",  new TypeVisitorFunction<String>(a->a.concat(a) ) {})
				.filter("*.foo", new TypeVisitorFunction<>(String.class, a->a.substring(1)))
				.build();
		String json = Json.toString(sample, filter);
		assertEquals(json,"{\"foo\":\"BAR\",\"a\":{\"foo\":\"ARCAR\",\"b\":{\"foo\":\"JARJAR\"}},\"c\":{\"foo\":\"AR\",\"d\":{\"foo\":\"PAR\"}}}");
	}

	@Test
	public void testReadTransformType() throws IOException {
		Map input = map(
				"foos", "a,b,c,d",
				"bars", List.of("frog", "toad"),
				"mars", "cars",
				"loos", "abc=123,def=456",
				"eeks", Map.of("aaaa", 9876, "bbbb", 5432),
				"leeks", List.of("a", "b", "c", "d")
				);

		String json = Json.toString(input);
		SomeType st = Json.fromString(json, SomeType.class, 
				new TypeParserFunction<>(String.class, Object.class, String::toUpperCase),
				new TypeParserFunction<String, List>(s -> Arrays.asList(s.split(",")) ){},
				new TypeParserFunction<String, Map>(s -> Arrays.stream(s.split(",")).map(k -> k.split("="))
						.collect(Collectors.toMap(a -> a[0], a -> a[1]))){},
				new TypeParserFunction<>(List.class, String.class, l -> {
					return (String) l.stream().map(Object::toString).collect(Collectors.joining(","));
				}),
				new TypeParserFunction<>(Map.class, Set.class, m -> new TreeSet(m.values())),
				new TypeParserFunction<List, Map>(l -> (Map) l.stream().collect(Collectors.toMap(s -> s, s -> s))){}
			);
		assertEquals(Json.toString(st, "  "), Json.toString(makeTestObject(), "  "));
	}

	private SomeType makeTestObject() {
		Map<String, String> loos = new LinkedHashMap<>();
		loos.put("ABC", "123");
		loos.put("DEF", "456");
		Map<String, String> leeks = new LinkedHashMap<>();
		leeks.put("A", "A");
		leeks.put("B", "B");
		leeks.put("C", "C");
		leeks.put("D", "D");
		Set<Integer> eeks = new TreeSet<>();
		eeks.add(5432);
		eeks.add(9876);
		return SomeType.builder()
				.foos(List.of("A", "B", "C", "D"))
				.loos(loos)
				.bars("FROG,TOAD")
				.mars("CARS")
				.eeks(eeks)
				.leeks(leeks)
				.build();
	}

	@Test
	public void testWriteTransformType() throws IOException {
		SomeType st = makeTestObject();
		String out = Json.toString(st,
				new TypeVisitorFunction<>(String.class, String::toLowerCase),
				new TypeVisitorFunction<>(Collection.class, t -> t.stream().map(Object::toString).collect(Collectors.joining(","))),
				new TypeVisitorFunction<>(Map.class, m -> ((Map<?,?>)m).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(Collectors.toList()))
				);
		assertEquals(out, "{\"bars\":\"frog,toad\",\"eeks\":\"5432,9876\",\"foos\":\"A,B,C,D\",\"leeks\":[\"a=a\",\"b=b\",\"c=c\",\"d=d\"],\"loos\":[\"abc=123\",\"def=456\"],\"mars\":\"cars\"}");
	}
	
	@Test public void testListTransform() throws IOException {
		List vals = List.of(1,2,3);
		String out = Json.toString(vals, PathVisitor.builder()
			.filter("[*]", new TypeVisitorFunction<Integer>(n -> (n)+10*(n)) {})
			.filter("[0]",  new TypeVisitorFunction<>(Integer.class,n -> (n)*100))
			.filter("[1]",  new TypeVisitorFunction<Integer>(n -> (n)*10) {})
				.build());
		assertEquals(out, "[1100,220,33]");
	}
	
	@Test public void testNameTransform() throws IOException{
		Map data = map("test-thing","123","other-thing","456");
		String json = Json.toString(data, new RenameVisitor(Map.of("test-thing", "testThing")));
		assertEquals("{\"testThing\":\"123\",\"other-thing\":\"456\"}",json);
		Object rt = Json.fromString(json, new RenameParser(Map.of("testThing","test-thing")));
		assertEquals(rt, data);
	}
	
	@Test public void testWriteLimit() {
		List vals = Arrays.asList(1,2, Arrays.asList(3,31,32,Arrays.asList(33,331,332,333,334,335,336,337),34,35,36,37),4,5,6,7);
		String json = Json.toString(vals, new LimitVisitor(5));
		assertEquals(json, "[1,2,[3,31,32,[33,331,332,333,334],34],4,5]");
		json = Json.toString(vals, PathVisitor.builder()
				.filter("[*]", new LimitVisitor(4))
				.filter("[*][*]", new LimitVisitor(6))
				.filter("[*][*][*]", new LimitVisitor(3))
				.build());
		assertEquals(json, "[1,2,[3,31,32,[33,331,332],34,35],4]");
	}

	@Test public void testReadLimit() throws IOException {
		String value = "[1,2,3,4,5,6,7,8,9]";
		List vals = Json.fromString(value, List.class, new LimitParser(4));
		assertEquals(vals, Arrays.asList(1,2,3,4));
		value = "[1,2,[3,31,32,[33,331,332,333,334,335,336,337],34,35,36,37],4,5,6,7]";
		vals = Json.fromString(value, List.class, PathParser.builder()
				.filter("[*]", new LimitParser(4))
				.filter("[*][*]", new LimitParser(6))
				.filter("[*][*][*]", new LimitParser(3))
				.build());
		assertEquals(vals,Arrays.asList(1,2, Arrays.asList(3,31,32,Arrays.asList(33,331,332),34,35),4));
	}
	
	@Test public void testReadFilterOmitPrecedence() throws IOException {
		String value = "[1,2,null,3,null,4,5,6,7,8,9]";
		List vals = Json.fromString(value, List.class, new LimitParser(5), new OmitNullParser());
		assertEquals(vals, Arrays.asList(1,2,3));
		vals = Json.fromString(value, List.class, new OmitNullParser(), new LimitParser(5));
		assertEquals(vals, Arrays.asList(1,2,3,4,5));
	}

	@Test public void testWriteFilterOmitPrecedence() {
		List vals = Arrays.asList("a","b",null,"c",null,"d");
		String json = Json.toString(vals, new LimitVisitor(5), new OmitNullVisitor());
		assertEquals(json, "[\"a\",\"b\",\"c\"]");
		json = Json.toString(vals, new OmitNullVisitor(), new LimitVisitor(5));
		assertEquals(json, "[\"a\",\"b\",\"c\",\"d\"]");
	}

	@Test public void testPathFilters() {
		Map vals = new LinkedHashMap<>();
		vals.put("abc","123");
		vals.put("jkl",null);
		vals.put("xyz","456");
		SomeType val = SomeType.builder()
				.foos(Arrays.asList("a",null,"b"))
				.loos(vals)
				.leeks(vals)
				.build();
		String json = Json.toString(val, PathVisitor.builder()
				.filter("*", new OmitNullVisitor())
				.filter("loos.*", new ExcludeVisitor("abc"))
				.filter("leeks.*", new OmitNullVisitor())
				.build());
		assertEquals(json, "{\"foos\":[\"a\",null,\"b\"],\"leeks\":{\"abc\":\"123\",\"xyz\":\"456\"},\"loos\":{\"jkl\":null,\"xyz\":\"456\"}}");
	}

	@Test
	public void testPathInTypeFilter() {
		Map vals = new LinkedHashMap<>();
		vals.put("abc", "def");
		vals.put("ghi", "jkl");
		List sub = new ArrayList<>();
		Map subval = new LinkedHashMap<>();
		subval.put("abc", "xyz");
		sub.add(subval);
		vals.put("pqr", sub);
		sub.add("mno");
		FilterVisitor toUpper = new FilterVisitor() {
			public void visitObjectField(Object name, Object value) {
				visitor.visitObjectField(name, value.toString().toUpperCase());
			}
		};
		String json = Json.toString(vals,
				new TypeVisitor<>(Map.class, PathVisitor.builder().filter("**", toUpper).build()));

		assertEquals(json, "{\"abc\":\"DEF\",\"ghi\":\"JKL\",\"pqr\":\"[{ABC=XYZ}, MNO]\"}");
		json = Json.toString(vals,
				new TypeVisitor<>(Map.class, PathVisitor.builder().filter("**.abc", toUpper).build()));

		assertEquals(json, "{\"abc\":\"DEF\",\"ghi\":\"jkl\",\"pqr\":[{\"abc\":\"XYZ\"},\"mno\"]}");
	}

	@Test
	public void testFieldMap() throws IOException {
		Things things = Things.builder()
				.thing1(Thing1.builder().head("Red").body("Blue").build())
				.thing2(Thing2.builder().head("Green").body("Yellow").build())
				.build();
		String json = Json.toString(things, new FieldVisitorFunction<Head, String>("head", String::toUpperCase) {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\",\"head\":\"RED\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"GREEN\"}}");
		Things rt = Json.fromString(json, Things.class, new FieldParserFunction<Head, String, String>("head", String::toLowerCase){});
		assertEquals(rt.thing1.head, "red");
		assertEquals(rt.thing2.head, "green");
		json = Json.toString(things, new FieldVisitorFunction<Thing1, String>("head", String::toUpperCase) {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\",\"head\":\"RED\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"Green\"}}");
		rt = Json.fromString(json, Things.class, new FieldParserFunction<Thing1, String, String>("head", String::toLowerCase) {});
		assertEquals(rt.thing1.head, "red");
		assertEquals(rt.thing2.head, "Green");
		json = Json.toString(things, new TypeVisitor<>(Thing1.class, new RenameVisitor("body", "abdomen")), new TypeVisitor<>(Thing2.class, new RenameVisitor("head", "thorax")), new FieldVisitorFunction<Thing2, String> ("thorax", String::toUpperCase) {});
		assertEquals(json, "{\"thing1\":{\"abdomen\":\"Blue\",\"head\":\"Red\"},\"thing2\":{\"body\":\"Yellow\",\"thorax\":\"GREEN\"}}");
		rt = Json.fromString(json, Things.class, new TypeParser<>(Thing1.class, new RenameParser("abdomen", "body")), new FieldParserFunction<Thing2, String, String>("head", String::toLowerCase) {}, new TypeParser<>(Thing2.class, new RenameParser("thorax", "head")));
		assertEquals(rt.thing1.body, "Blue");
		assertEquals(rt.thing2.head, "green");
	}

	@Test
	public void testTypedExcludes() {
		Things things = Things.builder()
				.thing1(Thing1.builder().head("Red").body("Blue").build())
				.thing2(Thing2.builder().head("Green").body("Yellow").build())
				.build();
		String json = Json.toString(things, new ExcludeVisitor("head"));
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\"}}");
		json = Json.toString(things, new TypeVisitor<>(Thing1.class, new ExcludeVisitor("head")));
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"Green\"}}");
	}

	@Test
	public void testTypedIncludes() throws IOException {
		Things things = Things.builder()
				.thing1(Thing1.builder().head("Red").body("Blue").build())
				.thing2(Thing2.builder().head("Green").body("Yellow").build())
				.build();
		String json = Json.toString(things, new IncludeVisitor<>(Json.models().get(Thing1.class),"body") {}, new IncludeVisitor<>(Json.models().get(Thing2.class),"body") {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\"}}");
		json = Json.toString(things,  new IncludeVisitor<>(Json.models().get(Thing1.class),"body") {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"Green\"}}");
		Things rt = Json.fromString(json, Things.class, new IncludeParser<>(Json.models().get(Thing2.class),"body") {});
		json = Json.toString(rt, new OmitNullVisitor());
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\"}}");
	}

	@Value
	@Builder
	private static final class SomeType {
		List<String> foos;
		Map<String, String> loos;
		String bars;
		String mars;
		Set<Integer> eeks;
		Map<String, String> leeks;
	}

	interface Head{
		String getHead();
	}

	@Value
	@Builder
	private static class Thing1 implements Head {
		String head;
		String body;
	}
	@Value
	@Builder
	private static class Thing2 implements Head {
		String head;
		String body;
	}
	@Value
	@Builder
	private static class Things{
		Thing1 thing1;
		Thing2 thing2;
	}

	private Map map(Object... keysAndValues) {
		Map m = new LinkedHashMap<>();
		for(int i=0; i< keysAndValues.length;i+=2) {
			m.put(keysAndValues[i], keysAndValues[i+1]);
		}
		return m;
	}

}
