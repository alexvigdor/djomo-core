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
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.parsers.PathParser;
import com.bigcloud.djomo.filter.visitors.InjectVisitor;
import com.bigcloud.djomo.filter.visitors.PathVisitor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PolyTest {
	Models Models = new Models();
	Json Json = new Json(Models);
	@Test
	public void testPoly() throws IOException{
		Poly poly = poly();
		String json = Json.toString(poly,"  ");
		Poly round = Json.read(new StringReader(json), Poly.class);
		assertEquals(round, poly);
	}
	
	private Poly poly() {
		return PolyFoo.builder()
				.name("rootFoo")
				.foo( 123)
				.children(List.of(
						PolyBar.builder()
								.name("branchBar")
								.bar( true)
								.children( List.of(PolyFoo.builder().name("leafFoo").build()) )
								.build()
						)
				)
				.namedChildren(map(
						"abc", PolyFoo.builder()
												.name("namedFoo")
												.foo( 99)
												.children(List.of(
													PolyBar.builder().name("namedLeafBar").build(),
													PolyFoo.builder().name("namedLeafFoo").build()
													)
												)
												.namedChildren(map(
														"xyz", PolyBar.builder()
																	.name("namedChildBar")
																	.children(new ArrayList<>())
																	.namedChildren( Map.of())
																	.build()
														)
												)
												.build()
						)
				).build();
	}
	
	@Test
	public void testArbitraryTypes() throws IOException, URISyntaxException {
		BaseVisitorFilter serialTransform = InjectVisitor.inject(Object.class, "@type", o -> o.getClass().getName());
		var deserialTransform = Filters.parseModel( (model, parser) -> {
			var o = parser.parse(model);
			if(o instanceof Map m) {
				Object type = m.get("@type");
				if(type==null) {
					return m;
				}
				Class c;
				try {
					c = Class.forName(type.toString());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				return Models.get(c).convert(m);
			}
			return o;
		});
		List data = List.of(
				new Coordinates(123, 456),
				new Names("John", "Doe")
				);
		String json = Json.toString(data, "  ", serialTransform);
		Object round = Json.read(new StringReader(json), Object.class, deserialTransform);
		assertEquals(round, data);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testStructuralChange() throws IOException {
		Poly poly =poly();
		PathVisitor serialTransform = PathVisitor.builder().filter("**.namedChildren", Filters.visitObject(Map.class, (object, model, visitor) -> 
			visitor.visit(
			((Map<?,?>)object).entrySet().stream().map(e->{
				Map kids = (Map) Models.get(Map.class).convert(e.getValue());
				kids.put("key", e.getKey());
				return kids;
			}).collect(Collectors.toList()))
		)).build();
		String json = Json.toString(poly,"  ", serialTransform);
		PathParser deserialTransform = PathParser.builder().filter("**.namedChildren", 
				Filters.parseModel((model, parser) -> {
					var list = ((List)parser.parse(Models.listModel));
					if(list == null) {
						return null;
					}
					return list.stream().collect(Collectors.toMap(m->((Map)m).get("key"), m->m));
				})
		
				).build();
		Poly round = Json.read(new StringReader(json), Poly.class, deserialTransform);
		assertEquals(round, poly);
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Coordinates{
		int x;
		int y;
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Names{
		String firstName;
		String lastName;
	}
	
	private Map map(Object... keysAndValues) {
		Map m = new LinkedHashMap<>();
		for(int i=0; i< keysAndValues.length;i+=2) {
			m.put(keysAndValues[i], keysAndValues[i+1]);
		}
		return m;
	}
}
