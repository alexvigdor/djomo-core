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
package com.bigcloud.djomo.rs.test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTestNg;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.rs.JsonBodyReader;
import com.bigcloud.djomo.rs.JsonBodyWriter;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;

public class TestSampleApp extends JerseyTestNg.ContainerPerClassTest {
	@Override
	protected Application configure() {
		return new ResourceConfig(SampleApp.class, SampleResolver.class, JsonBodyReader.class, JsonBodyWriter.class, ExceptionLogger.class);
	}
	
	@Override
	protected void configureClient(ClientConfig config) {
		config.register(JsonBodyReader.class);
		config.register(JsonBodyWriter.class);
	}

	@Test
	public void getThing1() {
		Thing thing1 = target("sample/thing1").request().get(Thing.class);
		Assert.assertEquals(thing1.name(), "thing1");
		Assert.assertEquals(thing1.elements(), List.of("a", "b", "c"));
	}
	
	@Test
	public void testEcho() {
		Thing myThing = new Thing("echo", Arrays.asList("a","b","c"));
		Thing rt = target("sample/echo").request().post(Entity.entity(myThing, "application/json"), Thing.class);
		Assert.assertEquals(rt, new Thing("ECHO", Arrays.asList("a","b","c")));
	}
	
	@Test
	public void testComplex() {
		Thing myThing = new Thing("complex", List.of( Map.of(1,List.of("2","3")), Map.of(4,List.of("5","6"))));
		String rt = target("sample/complex").request().post(Entity.entity(myThing, "application/json"), String.class);
		Assert.assertEquals(rt, "{\"elements\":[{\"1\":[2,3]},{\"4\":[5,6]}],\"name\":\"complex\"}");
	}
	
	@Test
	public void testFlatten() {
		String model = "[\"FLATTENED\",\"X\",\"Y\",\"Z\",null,[1,2,3]]";
		String rt = target("sample/flatten").request().post(Entity.entity(model, "application/json"), String.class);
		Assert.assertEquals(rt,  "[\"FLATTENED\",\"X\",\"Y\",\"Z\",[1,2,3]]");
	}

	@Test
	public void testConditionalAnnotation() {
		Thing thing2 = target("sample/canupper").request().get(Thing.class);
		Assert.assertEquals(thing2.name(), "thing2");
		Assert.assertEquals(thing2.elements(), Arrays.asList("a","b","c"));
		thing2 = target("sample/canupper").queryParam("toUpper", true).request().get(Thing.class);
		Assert.assertEquals(thing2.name(), "thing2");
		Assert.assertEquals(thing2.elements(), Arrays.asList("A","b","C"));
	}
	
	@Test 
	public void testDynamicExcludes() {
		String doc = target("sample/partial").request().get(String.class);
		Assert.assertEquals(doc, "{\"author\":\"Author\",\"body\":\"Body\",\"title\":\"Title\"}");
		doc = target("sample/partial").queryParam("exclude", "body").request().get(String.class);
		Assert.assertEquals(doc, "{\"author\":\"Author\",\"title\":\"Title\"}");
		doc = target("sample/partial").queryParam("exclude", "body", "author").request().get(String.class);
		Assert.assertEquals(doc, "{\"title\":\"Title\"}");
	}
	
	@Test
	public void testPretty() {
		String doc = target("sample/pretty").request().get(String.class);
		Assert.assertEquals(doc, "{\n"
				+ "	\"elements\" : [\n"
				+ "		\"x\",\n"
				+ "		\"y\",\n"
				+ "		\"z\"\n"
				+ "	],\n"
				+ "	\"name\" : \"pretty\"\n"
				+ "}");
	}
	
	@Test
	public void testLimit() {
		Thing myThing = new Thing("limit", Arrays.asList(1,2,Arrays.asList(3,31,32,33,34,35,36,37,38),4,5,6,7,8,9));
		String doc = target("sample/limit").request().post(Entity.entity(myThing, "application/json"), String.class);
		Assert.assertEquals(doc, "{\"elements\":[1,2,[3,31,32,33,34],4,5],\"name\":\"limit\"}");
	}

	@Test
	public void testPathExcludes() {
		Map data = map(
				"foo", map(
						"car", "dar",
						"far", "gar",
						"bar", "ear"),
				"bar", map(
						"nar", "mar",
						"bar", "jar"),
				"xyz", map(
						"foo", map("bar", "tar", "yar", "zar"),
						"bar",map("bar", "mar", "war", "var")));
		String rval = target("sample/foobar").request().post(Entity.entity(data, "application/json"), String.class);
		Assert.assertEquals(rval, "{\"foo\":{\"car\":\"dar\",\"far\":\"gar\"},\"bar\":{\"nar\":\"mar\",\"bar\":\"jar\"},\"xyz\":{\"foo\":{\"yar\":\"zar\"},\"bar\":{\"bar\":\"mar\",\"war\":\"var\"}}}");
	}

	@Test
	public void testExplode() {
		Thing myThing = new Thing("explode", Arrays.asList(1,2,3));
		String doc = target("sample/explode").request().post(Entity.entity(myThing, "application/json"), String.class);
		Assert.assertEquals(doc, "{\"name\":[\"e\",\"x\",\"p\",\"l\",\"o\",\"d\",\"e\"]}");
	}

	@Test
	public void testLabel() {
		Thing myThing = new Thing("label", Arrays.asList("hello", 123456, 123.456));
		String doc = target("sample/label").request().post(Entity.entity(myThing, "application/json"), String.class);
		Assert.assertEquals(doc, "{\"elements\":[\"(String) hello\",123456,\"(Double) 123.456\"],\"name\":\"label\"}");
	}

	@Test
	public void testReadLabel() {
		Thing myThing = new Thing("readlabel", Arrays.asList(123456, 123.456));
		String doc = target("sample/readlabel").request().post(Entity.entity(myThing, "application/json"), String.class);
		Assert.assertEquals(doc, "{\"elements\":[\"(Integer) 123456\",\"(Double) 123.456\"],\"name\":\"readlabel\"}");
	}
	
	@Test
	public void testSensitive() {
		Sensitive sensitive = new Sensitive("Psst");
		String doc = target("sample/sensitive").request().post(Entity.entity(sensitive, "application/json"), String.class);
		Assert.assertEquals(doc, "{\"classification\":\"4 secret characters\",\"message\":\"Psst\"}");
	}
	
	private Map map(Object... keysAndValues) {
		Map m = new LinkedHashMap<>();
		for(int i=0; i< keysAndValues.length;i+=2) {
			m.put(keysAndValues[i], keysAndValues[i+1]);
		}
		return m;
	}
}
