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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.ModelType;
import com.bigcloud.djomo.StaticType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

public class ParameterTest {
	Json Json = new Json();
	@Test
	public void testParameterizedList() throws IOException {
		List<Foo> foos = List.of(Foo.builder().name("john").build());
		String json = Json.toString(foos);
		var rt = Json.fromString(json, ModelType.of(List.class, Foo.class));
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testParameterizedStaticList() throws IOException {
		List<Foo> foos = List.of(Foo.builder().name("john").build());
		String json = Json.toString(foos);
		List<Foo> rt = Json.fromString(json, new StaticType<List<Foo>>() {
		});
		Assert.assertEquals(rt, foos);
	}
	
	@Test
	public void testParameterizedMap() throws IOException {
		Map<Integer, Foo> foos = Map.of(1, Foo.builder().name("john").build());
		String json = Json.toString(foos);
		var rt = Json.fromString(json, ModelType.of(Map.class, Integer.class, Foo.class));
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testParameterizedStaticMap() throws IOException {
		Map<Integer, Foo> foos = Map.of(1, Foo.builder().name("john").build());
		String json = Json.toString(foos);
		Map<Integer, Foo>  rt = Json.fromString(json, new StaticType<Map<Integer, Foo>>() {});
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testParameterizedClass() throws IOException {
		Thing<?, ?> foos = Thing.builder().thing(Foo.builder().name("john").build()).other(UUID.randomUUID()).build();
		String json = Json.toString(foos);
		var rt = Json.fromString(json, ModelType.of(Thing.class, Foo.class, UUID.class));
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testParameterizedStaticClass() throws IOException {
		Thing<?, ?> foos = Thing.builder().thing(Foo.builder().name("john").build()).other(UUID.randomUUID()).build();
		String json = Json.toString(foos);
		Thing<Foo, UUID> rt = Json.fromString(json, new StaticType<Thing<Foo, UUID>>() {
		});
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testNestedParameterizedList() throws IOException {
		List<Thing<?, ?>> foos = List
				.of(Thing.builder().thing(Foo.builder().name("john").build()).other(UUID.randomUUID()).build());
		String json = Json.toString(foos);
		var rt = Json.fromString(json, ModelType.of(List.class, ModelType.of(Thing.class, Foo.class, UUID.class)));
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testNestedParameterizedStaticList() throws IOException {
		List<Thing<?, ?>> foos = List
				.of(Thing.builder().thing(Foo.builder().name("john").build()).other(UUID.randomUUID()).build());
		String json = Json.toString(foos);
		List<Thing<Foo, UUID>> rt = Json.fromString(json, new StaticType<List<Thing<Foo, UUID>>>() {
		});
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testMapWithParameterizedValue() throws IOException {
		Map<UUID, Thing<?, ?>> foos = Map.of(UUID.randomUUID(),
				Thing.builder().thing(Foo.builder().name("john").build()).other(Instant.now()).build(),
				UUID.randomUUID(),
				Thing.builder().thing(Foo.builder().name("jim").build()).other(Instant.now()).build());
		String json = Json.toString(foos);
		var rt = Json.fromString(json,
				ModelType.of(Map.class, UUID.class, ModelType.of(Thing.class, Foo.class, Instant.class)));
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testMapWithParameterizedValueStatic() throws IOException {
		Map<UUID, Thing<?, ?>> foos = Map.of(UUID.randomUUID(),
				Thing.builder().thing(Foo.builder().name("john").build()).other(Instant.now()).build(),
				UUID.randomUUID(),
				Thing.builder().thing(Foo.builder().name("jim").build()).other(Instant.now()).build());
		String json = Json.toString(foos);
		Map<UUID, Thing<Foo, Instant>> rt = Json.fromString(json,
				new StaticType<Map<UUID, Thing<Foo, Instant>>>() {
				});
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testSubclass() throws IOException {
		List<Thing<?, ?>> foos = List.of(FancyThing.builder().thing(Foo.builder().name("john").build())
				.other(Instant.now()).fanciness(100).build());
		String json = Json.toString(foos);
		var rt = Json.fromString(json,
				ModelType.of(List.class, ModelType.of(FancyThing.class, Foo.class, Instant.class)));
		Assert.assertEquals(rt, foos);
	}

	@Test
	public void testSubclassStatic() throws IOException {
		List<Thing<?, ?>> foos = List.of(FancyThing.builder().thing(Foo.builder().name("john").build())
				.other(Instant.now()).fanciness(100).build());
		String json = Json.toString(foos);
		List<FancyThing<Foo, Instant>> rt = Json.fromString(json,
				new StaticType<List<FancyThing<Foo, Instant>>>() {
				});
		Assert.assertEquals(rt, foos);
	}

	@Value
	@Builder
	public static class Foo {
		String name;
	}

	@Value
	@NonFinal
	@SuperBuilder(toBuilder = true)
	public static class Thing<T, Z> {
		T thing;
		Z other;
	}

	@Value
	@SuperBuilder(toBuilder = true)
	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	public static class FancyThing<T, Z> extends Thing<T, Z> {
		int fanciness;
	}
}
