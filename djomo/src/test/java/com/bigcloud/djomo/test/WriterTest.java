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
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.filter.visitors.ExcludeVisitor;
import com.bigcloud.djomo.filter.visitors.OmitNullFieldVisitor;
import com.bigcloud.djomo.filter.visitors.OmitNullItemVisitor;
import com.bigcloud.djomo.filter.visitors.PathVisitor;
import com.bigcloud.djomo.test.ComplexModel.Direction;

import lombok.Builder;
import lombok.Value;

public class WriterTest {
	Json Json = new Json();
	@Test
	public void testJsonWriterDefaultOrder() {
		ImmutableModel m = ImmutableModel.builder().name("foo").enabled(true).count(5).build();
		String json = Json.toString(m);
		assertEquals(json, "{\"count\":5,\"enabled\":true,\"name\":\"foo\"}");
	}
	@Test
	public void testJsonWriterCustomOrder() {
		MutableModel m = MutableModel.builder().name("foo").enabled(true).count(5).build();
		String json = Json.toString(m);
		assertEquals(json, "{\"name\":\"foo\",\"enabled\":true,\"count\":5}");
	}
	@Test
	public void testJsonWriterTransform() {
		MutableModel m = MutableModel.builder().name("foo").enabled(true).count(5).build();
		String json = Json.toString(m,
				Filters.visitInt((i, visitor) -> visitor.visitInt(i*i)),
				Filters.visitBoolean((b, visitor) -> visitor.visitString(Boolean.valueOf(b).toString().toUpperCase())),
				PathVisitor.builder().filter("name", Filters.visitString((s, visitor) -> visitor.visit(map("value", s,"lang", "en"))))
				.build());
		assertEquals(json, "{\"name\":{\"value\":\"foo\",\"lang\":\"en\"},\"enabled\":\"TRUE\",\"count\":25}");
	}
	
	@Test
	public void testJsonWriterExclude() {
		MutableModel m = MutableModel.builder().name("foo").enabled(true).count(5).build();
		CharArrayWriter caWriter = new CharArrayWriter();
		String json = Json.toString(m, new ExcludeVisitor(MutableModel.class, "enabled"));
		assertEquals(json, "{\"name\":\"foo\",\"count\":5}");
	}

	@Test
	public void testEmptiness() {
		var data = map( 
				"a", null,
				"b", List.of(),
				"c", Map.of(),
				"d", "");
		String json = Json.toString(data);
		assertEquals(json, "{\"a\":null,\"b\":[],\"c\":{},\"d\":\"\"}");
	}
	@Test
	public void testJsonWriterNull() {
		MutableModel m = MutableModel.builder().build();
		String json = Json.toString(m);
		assertEquals(json, "{\"name\":null,\"enabled\":false,\"count\":0}");
	}
	
	@Test
	public void testJsonWriterOmitNull() {
		MutableModel m = MutableModel.builder().build();
		String json = Json.toString(m, new OmitNullFieldVisitor());
		assertEquals(json, "{\"enabled\":false,\"count\":0}");
	}
	
	@Test
	public void testJsonWriterNullList() {
		List<?> m = list("1", null, "2");
		String json = Json.toString(m);
		assertEquals(json, "[\"1\",null,\"2\"]");
	}
	
	@Test
	public void testJsonWriterOmitNullList() {
		List<?> m = list("1", null, "2");
		String json = Json.toString(m, new OmitNullItemVisitor());
		assertEquals(json, "[\"1\",\"2\"]");
	}

	@Test
	public void testJsonWriterComplex() {
		List<?> m = List.of(
				Basic.builder().title("abc").language("en").build(),
				Basic.builder().title("xyz").description("foo").build()
				);

		String json = Json.toString(m,
				new OmitNullFieldVisitor(), new ExcludeVisitor(Basic.class, "language"));
		assertEquals(json, "[{\"title\":\"abc\"},{\"description\":\"foo\",\"title\":\"xyz\"}]");
	}
	
	@Value
	@Builder
	static class Basic{
		String title;
		String description;
		String language;
	}
	
	@Test
	public void testComplexRoundTrip() throws IOException {
		ComplexModel testData = ComplexModel.builder()
				.direction(Direction.NORTH)
				.history(new EnumMap(Map.of(Direction.SOUTH, List.of(12345l,67890l), Direction.EAST, List.of(12345678912345l, 98765432198765l))))
				.models(List.of(
						ImmutableModel.builder().name("hello").count(99999).enabled(true).build(),
						ImmutableModel.builder().name("world").count(33333).enabled(false).build()
						))
				.children(Map.of("junior", Map.of("nick", "jr"), "senior", Map.of("nick", "sr")))
				.build();
		String json = Json.toString(testData);
		Object roundTrip = Json.read(new StringReader(json), ComplexModel.class);
		assertTrue(roundTrip != testData);
		assertEquals(roundTrip, testData);
	}
	
	@Test
	public void testDoubleWildcardPath(){
		List testData = list( 
				null,
				map( 
					"a", null,
					"b",map( 
							"a", "foo",
							"b", null)
					)
				)
				;
		String json = Json.toString(testData, new OmitNullFieldVisitor(), new OmitNullItemVisitor());
		assertEquals(json,"[{\"b\":{\"a\":\"foo\"}}]");
	}
	
	@Test void testEscapeChars() throws IOException {
		List<String> testData = List.of(
				"\"\r\n\t\f\b\\\\/",
				"\u2028\u2029\u0010\u0001\u0003\u001a");
		String json = Json.toString(testData);
		List<String> roundTrip = Json.fromString(json, new StaticType<List<String>>() {});
		assertTrue(roundTrip != testData);
		assertEquals(roundTrip, testData);
	}

	@Test
	public void testBinaryWrite() throws IOException {
		var data = List.of(1, 2, 3);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Json.write(data, baos);
		assertEquals(baos.toByteArray(), "[1,2,3]".getBytes());
		baos.reset();
		Json.write(data, baos, " ");
		assertEquals(baos.toByteArray(), "[\n 1,\n 2,\n 3\n]".getBytes());
	}

	@Test
	public void testWriter() throws IOException {
		var data = List.of(1, 2, 3);
		CharArrayWriter writer = new CharArrayWriter();
		Json.write(data, writer);
		assertEquals(writer.toString(), "[1,2,3]");
		writer.reset();
		Json.write(data, writer, " ");
		assertEquals(writer.toString(), "[\n 1,\n 2,\n 3\n]");
	}

	private Map map(Object... keysAndValues) {
		Map m = new LinkedHashMap<>();
		for(int i=0; i< keysAndValues.length;i+=2) {
			m.put(keysAndValues[i], keysAndValues[i+1]);
		}
		return m;
	}
	
	private List list(Object... items) {
		return Arrays.asList(items);
	}
}
