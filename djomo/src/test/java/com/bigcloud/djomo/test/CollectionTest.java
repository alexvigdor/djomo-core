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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.list.StreamMaker;

public class CollectionTest {
	@Test
	public void testMapType() throws IOException {
		String data = "{\"a\":\"1\",\"b\":{\"c\":\"2\",\"d\":3}}";
		Json json = new Json();
		Map parsed = (Map) json.fromString(data);
		Assert.assertEquals(parsed.getClass(), LinkedHashMap.class);
		Assert.assertEquals(parsed.get("b").getClass(), LinkedHashMap.class);
		json = new Json(Models.builder()
				.resolver(new Resolver.Substitute<>(Map.class, TreeMap.class))
				.build());
		parsed = (Map) json.fromString(data);
		Assert.assertEquals(parsed.getClass(), TreeMap.class);
		Assert.assertEquals(parsed.get("b").getClass(), TreeMap.class);
		parsed = json.fromString(data, ConcurrentHashMap.class);
		Assert.assertEquals(parsed.getClass(), ConcurrentHashMap.class);
		Assert.assertEquals(parsed.get("a").getClass(), String.class);
		Assert.assertEquals(parsed.get("b").getClass(), TreeMap.class);
		Assert.assertEquals(((Map)parsed.get("b")).get("c").getClass(), String.class);
		parsed = json.fromString(data, new StaticType<ConcurrentHashMap<String, Double>>() {});
		Assert.assertEquals(parsed.getClass(), ConcurrentHashMap.class);
		Assert.assertEquals(parsed.get("a").getClass(), Double.class);
		Assert.assertEquals(parsed.get("b").getClass(), TreeMap.class);
		Assert.assertEquals(((Map)parsed.get("b")).get("c").getClass(), String.class);
	}
	
	@Test
	public void testListType() throws IOException {
		String data = "[1,2,[3,4]]";
		Json json = new Json();
		List parsed = (List) json.fromString(data);
		Assert.assertEquals(parsed.getClass(), ArrayList.class);
		Assert.assertEquals(parsed.get(2).getClass(), ArrayList.class);
		json = new Json(Models.builder()
				.resolver(new Resolver.Substitute<>(List.class, LinkedList.class))
				.build());
		parsed = (List) json.fromString(data);
		Assert.assertEquals(parsed.getClass(), LinkedList.class);
		Assert.assertEquals(parsed.get(2).getClass(), LinkedList.class);
		parsed = json.fromString(data, Stack.class);
		Assert.assertEquals(parsed.getClass(), Stack.class);
		Assert.assertEquals(parsed.get(2).getClass(), LinkedList.class);
		Assert.assertEquals(parsed.get(1).getClass(), Integer.class);
		parsed = json.fromString(data, new StaticType<Vector<Double>>() {});
		Assert.assertEquals(parsed.getClass(), Vector.class);
		Assert.assertEquals(parsed.get(2).getClass(), LinkedList.class);
		Assert.assertEquals(parsed.get(1).getClass(), Double.class);
		Assert.assertEquals(((List)parsed.get(2)).get(0).getClass(), Integer.class);
	}
	
	@Test
	public void testGeneric() throws IOException {
		String data = "{\"a\":[{\"c\":{\"d\":\"e\"}}],\"b\":[{\"f\":\"g\"}]}";
		Json json = new Json();
		var parsed = json.fromString(data, new StaticType<TreeMap<String, List<Map>>>() {});
		Assert.assertEquals(parsed.getClass(), TreeMap.class);
		var c = parsed.get("a").get(0).get("c");
		Assert.assertEquals(c.getClass(), LinkedHashMap.class);
	}

	@Test
	public void testConvertCollection() throws IOException {
		Models models = new Models();
		String[] data = { "1", "2", "3", "4" };
		Model<List<Double>> model = models.get(new StaticType<ArrayList<Double>>() {});
		List<Double> converted = model.convert(data);
		Assert.assertEquals(converted.get(3), 4.0);
		Assert.assertEquals(model.convert(null), null);
		Assert.assertEquals(model.convert(converted), converted);
		Assert.assertEquals(models.get(ArrayList.class).convert(data), Arrays.asList(data));
		Assert.assertEquals(model.convert("3.0"), List.of(3.0));
	}

	@Test(expectedExceptions = NumberFormatException.class)
	public void testBadConvertCollection() throws IOException {
		Models models = new Models();
		Model<List<Double>> model = models.get(new StaticType<ArrayList<Double>>() {});
		model.convert("test");
	}

	@Test
	public void testConvertArray() throws IOException {
		Models models = new Models();
		List<String> data = List.of("1", "2", "3", "4");
		Model<double[]> model = models.get(double[].class);
		double[] converted = model.convert(data);
		Assert.assertEquals(converted[3], 4.0);
		Assert.assertEquals(model.convert(null), null);
		Assert.assertEquals(model.convert(converted), converted);
		Assert.assertEquals(model.convert("3.0"), new double[] { 3.0 });
	}

	@Test(expectedExceptions = NumberFormatException.class)
	public void testBadConvertArray() throws IOException {
		Models models = new Models();
		Model<double[]> model = models.get(double[].class);
		model.convert("test");
	}

	@Test
	public void testExpandArray() {
		Models models = new Models();
		String[] ar = { "1", "2" };
		var maker = ((ListModel<String[], ListMaker<String[], String>, String>) models.get(ar.getClass())).maker(ar);
		maker.item("3");
		Assert.assertEquals(maker.make(), new String[] { "1", "2", "3" });
	}

	@Test
	public void testStreams() {
		ListModel<Stream<Integer>, StreamMaker<Stream<Integer>,Integer>, Integer> streamModel = new Models().get(new StaticType<Stream<Integer>>() {});
		Assert.assertNull(streamModel.convert(null));
		var maker = streamModel.maker(Stream.of(1));
		maker.item(2);
		Stream<Integer> s = maker.make();
		Assert.assertEquals(streamModel.convert(s).collect(Collectors.toList()), List.of(1, 2));
		Assert.assertEquals(streamModel.convert("11").findFirst().orElseThrow(), 11);
	}
}
