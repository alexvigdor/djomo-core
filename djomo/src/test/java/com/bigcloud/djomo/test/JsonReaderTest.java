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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.ModelType;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.test.ComplexModel.Direction;

import lombok.Builder;
import lombok.Data;

public class JsonReaderTest {
	Json Json = new Json();
	@Test
	public void testTypedReader() throws IOException {
		String json="{\"name\":\"foo\",\"enabled\":true,\"count\":12345}";
		ImmutableModel model = Json.fromString(json, ImmutableModel.class);
		assertEquals(model.getName(), "foo");
		assertEquals(model.isEnabled(), true);
		assertEquals(model.getCount(), 12345);
	}
	
	@Test
	public void testComplexTypedReader() throws IOException{
		String json = "{\"children\":{\"john\":{\"age\":32,\"location\":[\"New York\",\"NY\",\"USA\"]}},\"direction\":\"NORTH\",\"history\":{\"NORTH\":[1,2,3],\"EAST\":[9,8,7]},\"models\":[{\"count\":0,\"enabled\":true,\"name\":\"foo\"}]}";
		ComplexModel model  = Json.fromString(json, ComplexModel.class);
		assertEquals(((Map)model.getChildren().get("john")).get("age"), 32);
		assertEquals(model
				.getHistory()
				.get(Direction.EAST)
				.get(0)
				.longValue(), 9);
	}
	
	@Test
	public void testSimpleListReader() throws IOException{
		String json = "[1,2,3.1,true,null,\"hi\"]";
		Object o = Json.fromString(json);
		assertTrue(o instanceof List);
		assertEquals(o, Arrays.asList(1,2,3.1,true,null,"hi"));
	}
	
	@Test
	public void testSimpleMapReader() throws IOException{
		String json = "{\"a\":1,\"b\":8765876587654321,\"c\":3.1,\"d\":true,\"e\":null,\"f\":\"hi\"}";
		Object o = Json.fromString(json);
		assertTrue(o instanceof Map);
		Map expected = new HashMap<>();
		expected.put("a", 1);
		expected.put("b", 8765876587654321l);
		expected.put("c", 3.1);
		expected.put("d", true);
		expected.put("e", null);
		expected.put("f", "hi");
		expected.put("e", null);
		assertEquals(o, expected);
	}
	
	@Test(expectedExceptions = RuntimeException.class)
	public void testBadReader() throws IOException {
		String json="{\"name\":\"foo\",\"enabled\":true,\"count\":12345";
		ImmutableModel model = Json.fromString(json, ImmutableModel.class);
	}

	@Test
	public void testEscapedString() throws IOException {
		String json="{\"name\":\"abcd\\\\efg\\\"ij\\u4f60k\\u{1f914}l\"}";
		MutableModel model = Json.fromString(json, MutableModel.class);
		assertEquals(model.getName(), "abcd\\efg\"ij你k樂l");
	}
	
	@Test
	public void testTypedKeys() throws IOException {
		Weirdo weirdo = Weirdo.builder().data(Map.of(1l, true, 2l, false)).build();
		String json = Json.toString(weirdo);
		Weirdo parsed = Json.read(new StringReader(json), Weirdo.class);
		assertEquals(weirdo.data, parsed.data);
	}

	@Test
	public void testBinaryRead() throws IOException {
		byte[] data = "{\"1\":\"a\",\"2\":\"b\"}".getBytes(StandardCharsets.UTF_8);
		var m1 = Json.read(data);
		assertEquals(m1, Map.of("1", "a", "2", "b"));
		m1 = Json.read(new ByteArrayInputStream(data));
		assertEquals(m1, Map.of("1", "a", "2", "b"));
		var m2 = Json.read(data, new StaticType<Map<Long, Character>>() {});
		assertEquals(m2, Map.of(1l, 'a', 2l, 'b'));
		m2 = Json.read(new ByteArrayInputStream(data), new StaticType<Map<Long, Character>>() {});
		assertEquals(m2, Map.of(1l, 'a', 2l, 'b'));
		var m3 = Json.read(data, ModelType.of(Map.class, Short.class, String.class));
		assertEquals(m3, Map.of((short) 1, "a", (short) 2, "b"));
		m3 = Json.read(new ByteArrayInputStream(data), ModelType.of(Map.class, Short.class, String.class));
		assertEquals(m3, Map.of((short) 1, "a", (short) 2, "b"));
		var m4 = Json.read(data, Map.class);
		assertEquals(m4, Map.of("1", "a", "2", "b"));
		m4 = Json.read(new ByteArrayInputStream(data), Map.class);
		assertEquals(m4, Map.of("1", "a", "2", "b"));
		var m5 = Json.read(data, new HashMap());
		assertEquals(m5, Map.of("1", "a", "2", "b"));
		m5 = Json.read(new ByteArrayInputStream(data), new HashMap());
		assertEquals(m5, Map.of("1", "a", "2", "b"));
		var m6 = Json.read(new ByteArrayInputStream(data), (Object) null, (BaseParserFilter[]) null);
		assertEquals(m6, Map.of("1", "a", "2", "b"));
	}

	@Data
	@Builder
	public static class Weirdo{
		Map<Long,Boolean> data;
	}
}
