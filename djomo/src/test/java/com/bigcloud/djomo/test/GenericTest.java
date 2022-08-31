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
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;

public class GenericTest {
	Json Json = new Json();

	@Test
	public void testGeneric() throws IOException {
		String json = "{\"a\":\"123\", \"b\":[ \"true\", \"false\" ], \"c\":{\"2021-10-15\":\"987\",\"2021-10-16\":\"654\"}}";
		Foo<Integer, Boolean, LocalDate> foo = Json.fromString(json, new StaticType<Foo<Integer, Boolean, LocalDate>>(){});
		Assert.assertEquals(Json.toString(foo), "{\"a\":123,\"b\":[true,false],\"c\":{\"2021-10-15\":987,\"2021-10-16\":654}}");
		Assert.assertEquals(foo.c.keySet().stream().findFirst().get().getClass(), LocalDate.class);
	}
	
	public static class Foo<A, B, C>{
		public A a;
		public List<B> b;
		public Map<C, A> c;
	}
}
