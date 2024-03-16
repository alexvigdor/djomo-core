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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.parsers.FieldParser;
import com.bigcloud.djomo.filter.parsers.IncludeParser;
import com.bigcloud.djomo.filter.parsers.LimitParser;
import com.bigcloud.djomo.filter.parsers.OmitNullItemParser;
import com.bigcloud.djomo.filter.parsers.PathParser;
import com.bigcloud.djomo.filter.parsers.RenameParser;
import com.bigcloud.djomo.filter.parsers.TypeParser;
import com.bigcloud.djomo.filter.visitors.ExcludeVisitor;
import com.bigcloud.djomo.filter.visitors.FieldVisitor;
import com.bigcloud.djomo.filter.visitors.IncludeVisitor;
import com.bigcloud.djomo.filter.visitors.LimitVisitor;
import com.bigcloud.djomo.filter.visitors.OmitNullFieldVisitor;
import com.bigcloud.djomo.filter.visitors.OmitNullItemVisitor;
import com.bigcloud.djomo.filter.visitors.PathVisitor;
import com.bigcloud.djomo.filter.visitors.RenameVisitor;
import com.bigcloud.djomo.filter.visitors.TypeVisitor;

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
				.filter("**.foo", Filters.visitString((s, visitor) -> visitor.visitString(s.toString().toUpperCase())))
				.filter("a.**.foo",  Filters.visitString((s, visitor) -> visitor.visitString(s.toString().concat(s.toString()))))
				.filter("*.foo", Filters.visitString((s, visitor) -> visitor.visitString(s.subSequence(1, s.length()))))
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
				Filters.parseString(parser -> parser.parseString().toString().toUpperCase()),
				new FieldParser<SomeType>("foos", Filters.parseModel((model, parser) -> 
						Arrays.asList(parser.parseString().toString().split(","))
					)) {},
				new FieldParser<SomeType>("loos", Filters.parseModel((model, parser) ->
						Arrays.stream(parser.parseString().toString().split(",")).map(k -> k.split("=")).collect(Collectors.toMap(a -> a[0], a -> a[1]))
					)) {},
				new FieldParser<SomeType>("bars", Filters.parseString(parser -> 
				Json.models().listModel.parse(parser).stream().map(Object::toString).collect(Collectors.joining(",")).toString())) {},
				new FieldParser(SomeType.class, "eeks", Filters.parseModel((model, parser) ->  new TreeSet(Json.models().mapModel.parse(parser).values()) )),
				new FieldParser(SomeType.class, "leeks", Filters.parseModel(Map.class, (model, parser) -> 
					Json.models().listModel.parse(parser).stream().collect(Collectors.toMap(s -> s, s -> s))))
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
				Filters.visitString((s, visitor) -> visitor.visitString(s.toString().toLowerCase())),
				Filters.visitList((list, model, visitor) -> visitor.visitString(model.stream(list).map(Object::toString).collect(Collectors.joining(",")).toString())),
				Filters.visitObject(Map.class, (object, model, visitor) -> 
					visitor.visitList( ((Map<?,?>)object).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(Collectors.toList()), Json.models().listModel)
				));
		assertEquals(out, "{\"bars\":\"frog,toad\",\"eeks\":\"5432,9876\",\"foos\":\"A,B,C,D\",\"leeks\":[\"a=a\",\"b=b\",\"c=c\",\"d=d\"],\"loos\":[\"abc=123\",\"def=456\"],\"mars\":\"cars\"}");
	}
	
	@Test public void testListTransform() throws IOException {
		List vals = List.of(1,2,3);
		String out = Json.toString(vals, PathVisitor.builder()
			.filter("[*]", Filters.visitInt((n, visitor) -> visitor.visitInt((n)+10*(n))))
			.filter("[0]",  Filters.visitInt((n, visitor) -> visitor.visitInt(100*(n))))
			.filter("[1]",  Filters.visitInt((n, visitor) -> visitor.visitInt(10*(n))))
				.build());
		assertEquals(out, "[1100,220,33]");
	}
	
	@Test
	public void testNameTransform() throws IOException {
		Map data = map("test-thing", "123", "other-thing", "456");
		String json = Json.toString(data, Filters.visitObjectField(
				(name, visitor) -> visitor.visitObjectField("test-thing".equals(name) ? "testThing" : name)));
		assertEquals("{\"testThing\":\"123\",\"other-thing\":\"456\"}", json);
		Object rt = Json.fromString(json, Filters.parseObjectField((model, field, parser) -> parser
				.parseObjectField(model, field.equals("testThing") ? "test-thing" : field)

		));
		assertEquals(rt, data);
	}
	
	@Test public void testWriteLimit() {
		List vals = Arrays.asList(1,2, Arrays.asList(3,31,32,Arrays.asList(33,331,332,333,334,335,336,337),34,35,36,37),4,5,6,7);
		String json = Json.toString(vals, new LimitVisitor(5));
		assertEquals(json, "[1,2,[3,31,32,[33,331,332,333,334],34],4,5]");
		json = Json.toString(vals, PathVisitor.builder()
				.filter("", new LimitVisitor(4))
				.filter("[*]", new LimitVisitor(6))
				.filter("[*][*]", new LimitVisitor(3))
				.build());
		assertEquals(json, "[1,2,[3,31,32,[33,331,332],34,35],4]");
	}

	@Test public void testReadLimit() throws IOException {
		String value = "[1,2,3,4,5,6,7,8,9]";
		List vals = Json.fromString(value, List.class, new LimitParser(4));
		assertEquals(vals, Arrays.asList(1,2,3,4));
		value = "[1,2,[3,31,32,[33,331,332,333,334,335,336,337],34,35,36,37],4,5,6,7]";
		vals = Json.fromString(value, List.class, PathParser.builder()
				.filter("", new LimitParser(4))
				.filter("[*]", new LimitParser(6))
				.filter("[*][*]", new LimitParser(3))
				.build());
		assertEquals(vals,Arrays.asList(1,2, Arrays.asList(3,31,32,Arrays.asList(33,331,332),34,35),4));
	}
	
	@Test public void testReadFilterOmitPrecedence() throws IOException {
		String value = "[1,2,null,3,null,4,5,6,7,8,9]";
		List vals = Json.fromString(value, List.class, new LimitParser(5), new OmitNullItemParser());
		assertEquals(vals, Arrays.asList(1,2,3));
		vals = Json.fromString(value, List.class, new OmitNullItemParser(), new LimitParser(5));
		assertEquals(vals, Arrays.asList(1,2,3,4,5));
	}

	@Test public void testWriteFilterOmitPrecedence() {
		List vals = Arrays.asList("a","b",null,"c",null,"d");
		String json = Json.toString(vals, new LimitVisitor(5), new OmitNullItemVisitor());
		assertEquals(json, "[\"a\",\"b\",\"c\"]");
		json = Json.toString(vals, new OmitNullItemVisitor(), new LimitVisitor(5));
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
				.filter(".", new OmitNullFieldVisitor())
				.filter("loos", Filters.visitObject((object, model, visitor) -> 
					visitor.visitObject(excludeABC((Map<?,?>)object), model)))
				.filter("leeks", new OmitNullFieldVisitor())
				.build());
		assertEquals(json, "{\"foos\":[\"a\",null,\"b\"],\"leeks\":{\"abc\":\"123\",\"xyz\":\"456\"},\"loos\":{\"jkl\":null,\"xyz\":\"456\"}}");
	}
	
	private Map excludeABC(Map map) {
		Map nmap = new LinkedHashMap<>(map);
		nmap.remove("abc");
		return nmap;
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
		BaseVisitorFilter toUpper = new BaseVisitorFilter() {
			@Override
			public void visitString(CharSequence value) {
				visitor.visitString(value.toString().toUpperCase());
			}
			@Override
			public void visitList(Object o, ListModel m) {
				visitString(o.toString());
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
		String json = Json.toString(things, new FieldVisitor<Head>("head", Filters.visitString((str, visitor) -> visitor.visitString(str.toString().toUpperCase()))) {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\",\"head\":\"RED\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"GREEN\"}}");
		ParserFilter toLower = Filters.parseString(parser -> parser.parseString().toString().toLowerCase() );
		Things rt = Json.fromString(json, Things.class, 
				new FieldParser<Head>("head", toLower) {}
				//new FieldParserFunction<Head, String, String>("head", String::toLowerCase){}
		);
		assertEquals(rt.thing1.head, "red");
		assertEquals(rt.thing2.head, "green");
		json = Json.toString(things, new FieldVisitor<Thing1>("head", Filters.visitString((str, visitor) -> visitor.visitString(str.toString().toUpperCase()))) {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\",\"head\":\"RED\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"Green\"}}");
		rt = Json.fromString(json, Things.class, new FieldParser<Thing1>("head", toLower) {});
		assertEquals(rt.thing1.head, "red");
		assertEquals(rt.thing2.head, "Green");
		json = Json.toString(things, 
				new RenameVisitor(Thing1.class, "body", "abdomen"),
				new RenameVisitor(Thing2.class, "head", "thorax"),
				new FieldVisitor<Thing2> ("thorax", Filters.visitString((str, visitor) -> visitor.visitString(str.toString().toUpperCase()))) {});
		assertEquals(json, "{\"thing1\":{\"abdomen\":\"Blue\",\"head\":\"Red\"},\"thing2\":{\"body\":\"Yellow\",\"thorax\":\"GREEN\"}}");
		rt = Json.fromString(json, Things.class, new RenameParser(Thing1.class, "abdomen", "body"), new FieldParser<Thing2>("head", toLower) {}, new RenameParser(Thing2.class, "thorax", "head"));
		assertEquals(rt.thing1.body, "Blue");
		assertEquals(rt.thing2.head, "green");
	}

	@Test
	public void testTypedExcludes() {
		Things things = Things.builder()
				.thing1(Thing1.builder().head("Red").body("Blue").build())
				.thing2(Thing2.builder().head("Green").body("Yellow").build())
				.build();
		String json = Json.toString(things, new ExcludeVisitor(Head.class, "head"));
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\"}}");
		json = Json.toString(things, new ExcludeVisitor(Thing1.class, "head"));
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"Green\"}}");
	}

	@Test
	public void testTypedIncludes() throws IOException {
		Things things = Things.builder()
				.thing1(Thing1.builder().head("Red").body("Blue").build())
				.thing2(Thing2.builder().head("Green").body("Yellow").build())
				.build();
		String json = Json.toString(things, new IncludeVisitor<>(Thing1.class,"body") {}, new IncludeVisitor<>(Thing2.class,"body") {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\"}}");
		json = Json.toString(things,  new IncludeVisitor<>(Thing1.class,"body") {});
		assertEquals(json, "{\"thing1\":{\"body\":\"Blue\"},\"thing2\":{\"body\":\"Yellow\",\"head\":\"Green\"}}");
		Things rt = Json.fromString(json, Things.class, new IncludeParser<>(Thing2.class,"body") {});
		json = Json.toString(rt, new OmitNullFieldVisitor());
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
