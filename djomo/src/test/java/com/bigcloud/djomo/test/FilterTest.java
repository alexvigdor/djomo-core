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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.filter.CircularReferenceVisitor;
import com.bigcloud.djomo.filter.ExcludeParser;
import com.bigcloud.djomo.filter.ExcludeVisitor;
import com.bigcloud.djomo.filter.FieldParser;
import com.bigcloud.djomo.filter.FieldParserFunction;
import com.bigcloud.djomo.filter.FieldVisitor;
import com.bigcloud.djomo.filter.FieldVisitorFunction;
import com.bigcloud.djomo.filter.FilterParser;
import com.bigcloud.djomo.filter.FilterVisitor;
import com.bigcloud.djomo.filter.IncludeParser;
import com.bigcloud.djomo.filter.IncludeVisitor;
import com.bigcloud.djomo.filter.MultiFilterParser;
import com.bigcloud.djomo.filter.MultiFilterVisitor;
import com.bigcloud.djomo.filter.OmitNullVisitor;
import com.bigcloud.djomo.filter.RenameParser;
import com.bigcloud.djomo.filter.RenameVisitor;
import com.bigcloud.djomo.filter.TypeParser;
import com.bigcloud.djomo.filter.TypeVisitor;
import com.bigcloud.djomo.filter.TypeVisitorTransform;

public class FilterTest {
	Json Json = new Json();
	
	List regularList;
	List circularList;
	Map regularObject;
	Map circularObject;
	
	@BeforeClass
	public void setup() {
		regularList = new ArrayList<>();
		circularList = new ArrayList<>();
		regularList.add("a");
		circularList.add("a");
		circularList.add(circularList);
		regularObject = new HashMap<>();
		circularObject = new HashMap<>();
		Map indirectObject = new HashMap();
		indirectObject.put("c", "d");
		regularObject.put("a", "b");
		regularObject.put("e", indirectObject);
		Map indirectCircularObject = new HashMap();
		indirectCircularObject.put("c", "d");
		indirectCircularObject.put("f", circularObject);
		circularObject.put("a", "b");
		circularObject.put("e", indirectCircularObject);
	}

	@Test(expectedExceptions = StackOverflowError.class)
	public void testCircularReferenceFilterObjectFail() {
		Json.toString(regularObject);
		Json.toString(circularObject);
	}
	
	@Test
	public void testCircularReferenceFilterObject() {
		Json.toString(regularObject, new CircularReferenceVisitor());
		Json.toString(circularObject, new CircularReferenceVisitor());
	}
	
	@Test(expectedExceptions = StackOverflowError.class)
	public void testCircularReferenceFilterListFail() {
		Json.toString(regularList);
		Json.toString(circularList);
	}
	
	@Test
	public void testCircularReferenceFilterList() {
		Json.toString(regularList, new CircularReferenceVisitor());
		Json.toString(circularList, new CircularReferenceVisitor());
	}

	public record Gadget(String name, Gear gear, Gauge gauge) {}

	public record Gear(String name, Double value, Double radius) {}

	public record Gauge(String name, Double value, Double max) {}

	@Visit(value = IncludeVisitor.class, type = Gauge.class, arg = { "name", "value" })
	@Visit(value = ExcludeVisitor.class, type = Gear.class, arg = "value")
	@Visit(value = RenameVisitor.class, arg = { "name", "n" }, path = { "gauge.*", "gear.*" })
	@Visit(OmitNullVisitor.class)
	@Parse(value = RenameParser.class, arg = { "n", "name" }, path = { "gauge.*", "gear.*" })
	@Parse(value = ExcludeParser.class, type = Gauge.class, arg = "value")
	public class GadgetFilters {}

	@Test
	public void testAnnotations() throws IOException {
		Json json = Json.builder().scan(GadgetFilters.class).build();
		Gadget gadget = new Gadget("gizmo", new Gear("spur", 13.7, 8.0), new Gauge("pressure", 15.0, 20.0));
		String str = json.toString(gadget);
		Assert.assertEquals(str,
				"{\"gauge\":{\"n\":\"pressure\",\"value\":15.0},\"gear\":{\"n\":\"spur\",\"radius\":8.0},\"name\":\"gizmo\"}");
		Gadget roundTrip = json.fromString(str, Gadget.class);
		Assert.assertEquals(json.toString(roundTrip),
				"{\"gauge\":{\"n\":\"pressure\"},\"gear\":{\"n\":\"spur\",\"radius\":8.0},\"name\":\"gizmo\"}");
	}
	@Test
	public void testConstructorMatching() {
		Json json = new Json();
		FilterParser[] filters = json.getAnnotationProcessor().parserFilters(CustomFilter.class);
		Assert.assertEquals(filters.length, 1);
		Assert.assertEquals(filters[0].getClass(), CustomFilter.class);
		CustomFilter cf = (CustomFilter) filters[0];
		Assert.assertEquals(cf.clazz, Date.class);
		Assert.assertEquals(cf.models, json.models());
		Assert.assertEquals(cf.method, "method");
		Assert.assertEquals(cf.arg[0],  "arg1");
	}

	@Parse(value = CustomFilter.class, type = Date.class, arg = { "method", "arg1" })
	public static class CustomFilter extends FilterParser {
		Class clazz;
		Models models;
		String method;
		String[] arg;

		public CustomFilter(Model model, Class clazz) {
			// bogus constructor to test matching
			this.clazz = clazz;
		}

		public CustomFilter(Class clazz, Model model) {
			// bogus constructor to test matching
			this.clazz = clazz;
		}

		public CustomFilter(Class clazz, Models models, String method, String arg, String excess) {
			// bogus constructor to test matching
			this.clazz = clazz;
		}
		public CustomFilter(Class clazz, Models models, String method, String... arg) {
			this.clazz = clazz;
			this.models = models;
			this.method = method;
			this.arg = arg;
		}
	}

	@Test
	public void testMultiVisitor() {
		Json json = Json.builder().scan(IncludeAndRenameVisitor.class).build();
		Thing in = new Thing("foo", "bar");
		String str = json.toString(in);
		Assert.assertEquals(str, "{\"n\":\"OOF\"}");
	}

	@Visit(IncludeAndRenameVisitor.class)
	public static class IncludeAndRenameVisitor extends MultiFilterVisitor {
		public IncludeAndRenameVisitor(Models models) {
			super(new IncludeVisitor<>(models.get(Thing.class), "name"),
					new FieldVisitorFunction<>(Thing.class, String.class, "name", s -> new StringBuilder(s).reverse().toString()),
					new FieldVisitor<>(Thing.class, "name", new FilterVisitor() {
						public void visit(Object o) {
							if (o instanceof String s) {
								o = s.toUpperCase();
							}
							super.visit(o);
						}
					}),
					new TypeVisitor<Thing>(new RenameVisitor("name", "n")) {});
		}
	}

	@Test
	public void testMultiParser() throws IOException {
		Json json = Json.builder().scan(IncludeAndRenameParser.class).build();
		String data = "{\"n\":\"OOF\",\"type\":\"bar\"}";
		Thing parsed = json.fromString(data, Thing.class);
		Assert.assertEquals(parsed, new Thing("foo", null));
	}

	@Parse(IncludeAndRenameParser.class)
	public static class IncludeAndRenameParser extends MultiFilterParser {
		public IncludeAndRenameParser(Models models) {
			super(new TypeParser<Thing>(new RenameParser("n", "name")) {
			},
					new FieldParserFunction<>(Thing.class, String.class, String.class, "name", s -> new StringBuilder(s).reverse().toString()),
					new FieldParser<>(Thing.class, "name", new FilterParser() {
						public <T> T parse(Model<T> model) {
							T t = super.parse(model);
							if (model.getType() == String.class) {
								t = (T) ((String) t).toLowerCase();
							}
							return t;
						}
					}),
					new IncludeParser<>(models.get(Thing.class), "name"));
		}
	}

	public static record Thing(String name, String type) {}

	@Test
	public void testInjection() throws IOException {
		Dao<Thing> dao = new Dao<Thing>();
		List<UUID> ids = new ArrayList<>();
		List<Thing> things = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			UUID id = UUID.randomUUID();
			Thing thing = new Thing("Thing " + i, "thing");
			dao.put(id, thing);
			ids.add(id);
			things.add(thing);
		}
		Json json = Json.builder().inject(dao).scan(DaoLoader.class).build();
		String str = json.toString(ids);
		List<Thing> rt = json.fromString(str, new StaticType<List<Thing>>() {});
		Assert.assertEquals(rt, things);
	}

	@Visit(DaoLoader.class)
	public static class DaoLoader extends TypeVisitorTransform<UUID> {
		final Dao dao;

		public DaoLoader(Dao dao) {
			this.dao = dao;
		}

		@Override
		public Object transform(UUID in) {
			return dao.get(in);
		}

	}

	public static class Dao<T> {
		private ConcurrentHashMap<UUID, T> data = new ConcurrentHashMap<>();

		public T get(UUID id) {
			return data.get(id);
		}

		public void put(UUID id, T obj) {
			data.put(id, obj);
		}
	}
}
