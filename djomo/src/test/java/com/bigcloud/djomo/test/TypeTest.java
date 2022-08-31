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
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.filter.OmitNullVisitor;
import com.bigcloud.djomo.filter.TypeParserTransform;
import com.bigcloud.djomo.filter.TypeVisitorTransform;

import lombok.Builder;
import lombok.Value;

public class TypeTest {
	@Test
	public void testRecursiveType() {
		Recur test = new Recur();
		test.name= "a";
		test.recur = new Recur();
		test.recur.name="b";
		String json = new Json().toString(test, new OmitNullVisitor());
		assertEquals(json, "{\"name\":\"a\",\"recur\":{\"name\":\"b\"}}");
	}
	
	public static class Recur{
		public String name;
		public Recur recur;
	}
	
	@Parse(PersonParser.class)
	@Visit(PersonVisitor.class)
	public static class Person {
		public String firstName;
		public String lastName;
	}
	
	@Test
	public void testJsonAdapter() throws IOException{
		Models models = new Models();
		Json json = new Json(models);
		Person person = new Person();
		person.firstName = "John";
		person.lastName = "Von Doe";
		String out = json.toString(person);
		assertEquals(out, "{\"firstName\":\"John\",\"lastName\":\"Von Doe\"}");
		out = json.toString(person, new PersonVisitor());
		assertEquals(out, "\"John Von Doe\"");
		Person roundTrip = json.fromString(out, Person.class, new PersonParser());
		assertEquals(roundTrip.firstName, person.firstName);
		// scan for filters
		json = Json.builder().models(models).scan(Person.class).build();
		out = json.toString(person);
		assertEquals(out, "\"John Von Doe\"");
		roundTrip = json.fromString(out, Person.class);
		assertEquals(roundTrip.firstName, person.firstName);
		Map pmap = models.mapModel.convert(person);//.fromString("{\"firstName\":\"John\",\"lastName\":\"Von Doe\"}", Map.class);
		out = json.toString(pmap);
		assertEquals(out, "{\"firstName\":\"John\",\"lastName\":\"Von Doe\"}");
		Map mrt = json.fromString(out, Map.class);
		assertEquals(mrt, pmap);
	}
	
	public static class PersonVisitor extends TypeVisitorTransform<Person>{

		@Override
		public Object transform(Person person) {
			return person.firstName+" "+person.lastName;
		}
		
	}
	
	public static class PersonParser extends TypeParserTransform<String, Person>{

		@Override
		public Person transform(String in) {
			String[] parts = in.split("\\s+", 2);
			Person person = new Person();
			person.firstName = parts[0];
			if(parts.length>1) {
				person.lastName = parts[1];
			}
			return person;
		}
		
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testMissingType() {
		new Resolver() {

			@Override
			public Object resolve(Parser parser) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public <T> void testAmorphousInlineType() {
		new Resolver<T>() {

			@Override
			public T resolve(Parser parser) {
				return null;
			}
		};
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public <T> void testAmorphousType() {
		new ResolverImpl<T>();
	}

	class ResolverImpl<T> extends Resolver<T> {

		@Override
		public T resolve(Parser parser) {
			return null;
		}

	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public <T> void testAmorphousInlineMultiType() {
		new TypeParserTransform<String, T>() {

			@Override
			public T transform(String in) {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	@Test
	public void testAdHocResolver() throws IOException {
		Json json = new Json(Models.builder().resolver(new FurnitureResolver()).build());
		List<Furniture> furnishings = List.of(Table.builder().name("Kitchen table").legs(4).height(29).build(),
				Chair.builder().name("Bar stool").legs(3).build());
		String str = json.toString(furnishings);
		List<Furniture> roundTrip = json.fromString(str, new StaticType<List<Furniture>>() {
		});
		assertEquals(roundTrip, furnishings);
	}

	public static interface Furniture {
		String getName();

		int getLegs();
	}

	@Value
	@Builder
	public static class Chair implements Furniture {
		float seatHeight;
		float seatDepth;
		float backHeight;
		String name;
		int legs;
	}

	@Value
	@Builder
	public static class Table implements Furniture {
		float height;
		float width;
		float depth;
		String name;
		int legs;
	}

	public static class FurnitureResolver extends Resolver<Furniture> {
		Model<Chair> chairModel;
		Model<Table> tableModel;

		@Override
		public Furniture resolve(Parser parser) {
			Map data = parser.parse(parser.models().mapModel);
			if (data.containsKey("seatHeight")) {
				return chairModel.convert(data);
			}
			return tableModel.convert(data);
		}

		@Override
		public void init(ModelContext models, Type[] typeArgs) {
			chairModel = models.get(Chair.class);
			tableModel = models.get(Table.class);
		}
	}
}
