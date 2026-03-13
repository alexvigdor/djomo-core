/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.annotation.Embed;
import com.bigcloud.djomo.annotation.Order;
import com.bigcloud.djomo.api.ObjectModel;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

public class EmbedTests {
	Json json = new Json();

	@Value
	@Builder
	@Order("pointer")
	public static class BuilderEmbedder {
		String foo;
		@Embed
		ObjectPointer pointer;
		@Embed
		ObjectDescription description;
		@Embed
		Map<String, String> properties;
	}

	@Value
	@Builder
	public static class ObjectPointer {
		String type;
		String id;
	}

	@Value
	@Builder
	@Order("name")
	public static class ObjectDescription {
		String name;
		String description;
	}

	@Test
	public void testBuilderEmbedder() throws IOException {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("a", "b");
		props.put("c", "d");
		var be = BuilderEmbedder.builder()
				.foo("bar")
				.pointer(ObjectPointer.builder().id("id").type("type").build())
				.description(ObjectDescription.builder().name("name").description("description").build())
				.properties(props).build();
		String ser = json.toString(be);
		Assert.assertEquals(ser,
				"{\"id\":\"id\",\"type\":\"type\",\"name\":\"name\",\"description\":\"description\",\"foo\":\"bar\",\"a\":\"b\",\"c\":\"d\"}");
		var rt = json.fromString(ser, BuilderEmbedder.class);
		Assert.assertEquals(rt, be);
	}

	@Data
	public static class Envelope<T> {
		@Embed
		T value;
		UUID id;
		Instant timestamp;
	};

	public static record CustomerOrder(int orderNumber, @Embed Customer customer) {

	}

	@Data
	public static class Customer {
		String name;
		String address;
		@Embed
		EnumMap<AllowedExt, Integer> xt;
	}

	public static enum AllowedExt {
		foo,
		bar
	}

	public static class OrderEnvelope extends Envelope<CustomerOrder> {
	}

	@Test
	public void testEnvelope() throws IOException {
		Customer customer = new Customer();
		customer.name = "John Doe";
		customer.address = "123 Main St";
		customer.xt = new EnumMap<>(AllowedExt.class);
		customer.xt.put(AllowedExt.foo, 987);
		customer.xt.put(AllowedExt.bar, 654);
		CustomerOrder order = new CustomerOrder(456, customer);
		OrderEnvelope env = new OrderEnvelope();
		env.setValue(order);
		env.setId(UUID.fromString("f055db3a-ba63-4be6-80b9-6685627bed6b"));
		env.setTimestamp(Instant.ofEpochMilli(1000000000000l));
		String ser = json.toString(env);
		Assert.assertEquals(ser,
				"{\"id\":\"f055db3a-ba63-4be6-80b9-6685627bed6b\",\"timestamp\":\"2001-09-09T01:46:40Z\",\"orderNumber\":456,\"address\":\"123 Main St\",\"name\":\"John Doe\",\"foo\":987,\"bar\":654}");
		var rt = json.fromString(ser, OrderEnvelope.class);
		Assert.assertEquals(rt, env);
		var disp = json.fromString(
				"{\"id\":\"f055db3a-ba63-4be6-80b9-6685627bed6b\",\"timestamp\":\"2001-09-09T01:46:40Z\",\"orderNumber\":456,\"address\":\"123 Main St\",\"name\":\"John Doe\",\"foo\":987,\"bar\":654,\"car\":321}",
				OrderEnvelope.class);
		;
		Assert.assertEquals(disp, rt);
	}

	@Test
	public void testConvert() throws IOException {
		var raw = json.fromString(
				"{\"id\":\"f055db3a-ba63-4be6-80b9-6685627bed6b\",\"timestamp\":\"2001-09-09T01:46:40Z\",\"orderNumber\":456,\"address\":\"123 Main St\",\"name\":\"John Doe\",\"foo\":987,\"bar\":654,\"car\":321}");
		OrderEnvelope e = json.models().get(OrderEnvelope.class).convert(raw);
		String ser = json.toString(e);
		Assert.assertEquals(ser,
				"{\"id\":\"f055db3a-ba63-4be6-80b9-6685627bed6b\",\"timestamp\":\"2001-09-09T01:46:40Z\",\"orderNumber\":456,\"address\":\"123 Main St\",\"name\":\"John Doe\",\"foo\":987,\"bar\":654}");
		var map = json.models().mapModel.convert(e);
		ser = json.toString(map);
		Assert.assertEquals(ser,
				"{\"id\":\"f055db3a-ba63-4be6-80b9-6685627bed6b\",\"timestamp\":\"2001-09-09T01:46:40Z\",\"orderNumber\":456,\"address\":\"123 Main St\",\"name\":\"John Doe\",\"foo\":987,\"bar\":654}");
	}

	@Test
	public void testOrderMaker() throws IOException {
		OrderEnvelope env = json.fromString(
				"{\"id\":\"f055db3a-ba63-4be6-80b9-6685627bed6b\",\"timestamp\":\"2001-09-09T01:46:40Z\",\"orderNumber\":456,\"address\":\"123 Main St\",\"name\":\"John Doe\",\"foo\":987,\"bar\":654}",
				OrderEnvelope.class);
		ObjectModel<OrderEnvelope> model = json.models().get(OrderEnvelope.class);
		var maker = model.maker(env);
		var rt = model.make(maker);
		Assert.assertEquals(rt, env);
	}

	@Value
	@Builder
	@Order("name")
	public static class FixedEmbeds {
		String name;
		@Embed
		Modified modified;
	}

	public static record Modified(Instant lastModified, String lastModifiedBy) {
	};

	@Test
	public void testFixedEmbed() {
		var fixedEmbeds = FixedEmbeds.builder().name("main")
				.modified(new Modified(Instant.ofEpochMilli(1000000000000l), "user123")).build();
		String ser = json.toString(fixedEmbeds);
		Assert.assertEquals(ser,
				"{\"name\":\"main\",\"lastModified\":\"2001-09-09T01:46:40Z\",\"lastModifiedBy\":\"user123\"}");
		ObjectModel<FixedEmbeds> model = json.models().get(FixedEmbeds.class);
		var maker = model.maker(fixedEmbeds);
		var rt = model.make(maker);
		Assert.assertEquals(rt, fixedEmbeds);
	}
}
