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
import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.visitors.ListVisitor;
import com.bigcloud.djomo.api.visitors.ObjectVisitor;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.error.AnnotationException;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.filter.FilterListModel;
import com.bigcloud.djomo.filter.FilterFieldObjectModel;
import com.bigcloud.djomo.filter.parsers.ExcludeParser;
import com.bigcloud.djomo.filter.parsers.FieldParser;
import com.bigcloud.djomo.filter.parsers.IncludeParser;
import com.bigcloud.djomo.filter.parsers.MultiFilterParser;
import com.bigcloud.djomo.filter.parsers.RenameParser;
import com.bigcloud.djomo.filter.parsers.TypeParser;
import com.bigcloud.djomo.filter.visitors.CircularReferenceVisitor;
import com.bigcloud.djomo.filter.visitors.ExcludeVisitor;
import com.bigcloud.djomo.filter.visitors.FieldVisitor;
import com.bigcloud.djomo.filter.visitors.IncludeVisitor;
import com.bigcloud.djomo.filter.visitors.MultiFilterVisitor;
import com.bigcloud.djomo.filter.visitors.OmitNullFieldVisitor;
import com.bigcloud.djomo.filter.visitors.RenameVisitor;
import com.bigcloud.djomo.filter.visitors.TypeVisitor;

import lombok.EqualsAndHashCode;
import lombok.ToString;

public class FilterTest {
	Json json = new Json();
	
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
		json.toString(regularObject);
		json.toString(circularObject);
	}
	
	@Test
	public void testCircularReferenceFilterObject() {
		json.toString(regularObject, new CircularReferenceVisitor());
		json.toString(circularObject, new CircularReferenceVisitor());
	}
	
	@Test(expectedExceptions = StackOverflowError.class)
	public void testCircularReferenceFilterListFail() {
		json.toString(regularList);
		json.toString(circularList);
	}
	
	@Test
	public void testCircularReferenceFilterList() {
		json.toString(regularList, new CircularReferenceVisitor());
		json.toString(circularList, new CircularReferenceVisitor());
	}

	public interface Named{
		String name();
	}
	public record Gadget(String name, Gear gear, Gauge gauge) implements Named {}

	public record Gear(String name, Double value, Double radius) implements Named {}

	public record Gauge(String name, Double value, Double max) implements Named {}

	@Visit(value = IncludeVisitor.class, type = Gauge.class, arg = { "name", "value" })
	@Visit(value = ExcludeVisitor.class, type = Gear.class, arg = "value")
	@Visit(value = RenameVisitor.class, type=Named.class, arg = { "name", "n" }, path = { "gauge.**", "gear.**" })
	@Visit(OmitNullFieldVisitor.class)
	@Parse(value = RenameParser.class, type=Named.class, arg = { "n", "name" }, path = { "gauge.*", "gear.*" })
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
		ParserFilterFactory[] filters = json.getAnnotationProcessor().parserFilters(CustomFilter.class);
		Assert.assertEquals(filters.length, 1);
		Assert.assertEquals(filters[0].getClass(), CustomFilter.class);
		CustomFilter cf = (CustomFilter) filters[0];
		Assert.assertEquals(cf.clazz, Date.class);
		Assert.assertEquals(cf.models, json.models());
		Assert.assertEquals(cf.method, "method");
		Assert.assertEquals(cf.arg[0],  "arg1");
	}

	@Parse(value = CustomFilter.class, type = Date.class, arg = { "method", "arg1" })
	public static class CustomFilter extends BaseParserFilter {
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
		public IncludeAndRenameVisitor(Models models) throws IllegalAccessException {
			super(new IncludeVisitor<>(Thing.class, "name"),
					new FieldVisitor<>(Thing.class, "name", Filters.visitString((s,v)->v.visitString(new StringBuilder(s).reverse().toString().toUpperCase()))),
					new RenameVisitor(Thing.class, "name", "n"));
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
		public IncludeAndRenameParser(Models models) throws IllegalAccessException {
			super(
					new RenameParser(Thing.class, "n", "name"),
					new FieldParser<>(Thing.class, "name", Filters.parseString(
							parser -> new StringBuilder(parser.parseString()).reverse().toString().toLowerCase())),
					new IncludeParser<>(Thing.class, "name"));
		}
	}

	public static record Thing(String name, String type) {}

	@Test
	public void testInjection() throws IOException {
		Dao<Thing> dao = new Dao<Thing>();
		Things things = new Things();
		UUIDs uuids = new UUIDs();
		uuids.main  = UUID.randomUUID();
		things.main = new Thing("Main thing", "main");
		dao.put(uuids.main, things.main);
		for (int i = 0; i < 5; i++) {
			UUID id = UUID.randomUUID();
			Thing thing = new Thing("Thing " + i, "thing");
			dao.put(id, thing);
			uuids.data.add(id);
			things.data.add(thing);
		}
		Json json = Json.builder().inject(dao)
				.scan(DaoListLoader.class, DaoFieldLoader.class)
				.build();
		String str = json.toString(uuids);
		Things rt = json.fromString(str, Things.class);
		Assert.assertEquals(rt, things);
	}
	
	@EqualsAndHashCode
	@ToString
	public static class UUIDs {
		public UUID main;
		public List<UUID> data = new ArrayList<>();
	}
	@EqualsAndHashCode
	@ToString
	public static class Things {
		public Thing main;
		public List<Thing> data = new ArrayList<>();
	}

	@Test(expectedExceptions = AnnotationException.class)
	public void testMissingVisitorInjection() throws IOException {
		json.builder().scan(DaoListLoader.class).build();
	}

	@Visit(DaoListLoader.class)
	public static class DaoListLoader implements ListVisitor {
		final Dao dao;
		final Model<UUID> uuidModel;
		final ListModel listLoader;

		public DaoListLoader(Dao dao, Models models) {
			this.dao = dao;
			this.uuidModel = models.get(UUID.class);
			listLoader = new FilterListModel(models.listModel) {
				@Override
				public void visitItems(Object t, Visitor visitor) {
					listModel.stream(t).map(u -> dao.get((UUID)u)).forEach( i -> {
						if(i!=null) {
							visitor.visitListItem();
							visitor.visit(i);
						}
					});
				}
			};
		}

		@Override
		public void visitList(Object list, ListModel model, Visitor visitor) {
			if(model.itemModel() == uuidModel) {
				model = listLoader;
			}
			visitor.visitList(list, model);
		}

	}
	@Visit(value=DaoFieldLoader.class,type = UUIDs.class)
	public static class DaoFieldLoader implements ObjectVisitor {
		final Dao dao;
		final ObjectModel filterModel;
		
		public DaoFieldLoader(Dao dao, ObjectModel<UUIDs> model,  Models models) {
			this.dao = dao;
			Model<UUID> uuidModel = models.get(UUID.class);
			filterModel = new FilterFieldObjectModel<>(model, model.fields().stream()
					.map(field -> {
					if(field.model() == uuidModel) {
						return new FilterField(field) {
							
							@Override
							public void visit(Object source, Visitor visitor) {
								visitor.visitObjectField(field.key());
								visitor.visit(dao.get((UUID) field.get(source)));
							}
							
						};
					}
					return field;
				})
			);
			
		}
		

		@Override
		public void visitObject(Object object, ObjectModel model, Visitor visitor) {
			if(object instanceof UUIDs) {
				model = filterModel;
			}
			visitor.visitObject(object, model);
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

	@Test(expectedExceptions = AnnotationException.class)
	public void testMissingParserInjection() throws IOException {
		json.builder().scan(TroubleMaker.class).build();
	}

	@Parse(TroubleMaker.class)
	public static class TroubleMaker extends BaseParserFilter {
		public TroubleMaker(String name) {

		}

		public TroubleMaker(Dao dao, String name) {

		}
	}
}
