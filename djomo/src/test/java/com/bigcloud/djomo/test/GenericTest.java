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
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Filters;

public class GenericTest {
	Json json = new Json();

	@Test
	public void testGeneric() throws IOException {
		String data = "{\"a\":\"123\", \"b\":[ \"true\", \"false\" ], \"c\":{\"2021-10-15\":\"987\",\"2021-10-16\":\"654\"}}";
		var foo = json.fromString(data, new StaticType<Foo<Integer, Boolean, LocalDate>>() {}, 
					Filters.parseInt(p -> Integer.valueOf(p.parseString().toString())),
					Filters.parseBoolean(p -> Boolean.valueOf(p.parseString().toString()))
				);
		Assert.assertEquals(json.toString(foo),
				"{\"a\":123,\"b\":[true,false],\"c\":{\"2021-10-15\":987,\"2021-10-16\":654}}");
		Assert.assertEquals(foo.c.keySet().stream().findFirst().get().getClass(), LocalDate.class);
	}

	public static class Foo<A, B, C> {
		public A a;
		public List<B> b;
		public Map<C, A> c;
	}

	@Test
	public void testLowerBound() throws IOException {
		LowerBound bound = new LowerBound();
		bound.stuff = List.of(new Exception("test exception"), new RuntimeException("test runtime exception"),
				new Throwable("test throwable"));
		String out = json.toString(bound);
		LowerBound rt = json.fromString(out, LowerBound.class);
		Assert.assertEquals(rt.stuff.size(), 3);
		Assert.assertEquals(rt.stuff.stream().findFirst().orElseThrow().getClass(), Throwable.class);
	}

	public static class LowerBound {
		public Collection<? super RuntimeException> stuff;
	}

	@Test
	public void testUpperBound() throws IOException {
		UpperBound bound = new UpperBound();
		bound.stuff = List.of(new Exception("test exception"), new RuntimeException("test runtime exception"),
				new Throwable("test throwable"));
		String out = json.toString(bound);
		UpperBound rt = json.fromString(out, UpperBound.class);
		Assert.assertEquals(rt.stuff.size(), 3);
		Assert.assertEquals(rt.stuff.stream().findFirst().orElseThrow().getClass(), Throwable.class);
	}

	public static class UpperBound {
		public Collection<? extends Throwable> stuff;
	}

	@Test
	public void testGenericArray() throws IOException {
		Array<String> ar = new Array<>();
		ar.items = new String[] { "hello", "world" };
		String out = json.toString(ar);
		var rar = json.fromString(out, new StaticType<Array<String>>() {});
		Assert.assertEquals(rar.items, ar.items);
	}

	@Test
	public void testParameterizedGenericArray() throws IOException {
		CollectionArray<List<String>, String> lar = new CollectionArray<>();
		lar.items = new List[] { List.of("1", "2", "3"), List.of("a", "b", "c") };
		String out = json.toString(lar);
		var lrar = json.fromString(out, new StaticType<CollectionArray<List<String>, String>>() {});
		Assert.assertEquals(lrar.items, lar.items);
	}

	public static class Array<T> {
		public T[] items;
	}

	public static class CollectionArray<C extends Collection<T>, T> {
		public C[] items;
	}
}
