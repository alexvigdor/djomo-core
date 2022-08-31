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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Model;

public class SupplierTest {
	@Test
	public void testSupplier() throws IOException {
		List<Integer> data = List.of(1, 2, 3);
		Supplier<Stream<Map<Integer, String>>> streamSupplier = () -> data.stream().map(i -> Map.of(i, "Item " + i));
		Json json = new Json();
		String str = json.toString(streamSupplier);
		Assert.assertEquals(str, "[{\"1\":\"Item 1\"},{\"2\":\"Item 2\"},{\"3\":\"Item 3\"}]");
		Supplier<Stream<Map<Integer, String>>> rt = json.fromString(str, new StaticType<Supplier<Stream<Map<Integer, String>>>>(){});
		String str2 = json.toString(rt);
		Assert.assertEquals(str2, str);
	}

	@Test
	public void testTypedConvert() {
		Model<Supplier<Integer>> model = new Models().get(new StaticType<Supplier<Integer>>() {});
		Supplier<Integer> supplier = model.convert("1");
		Assert.assertEquals(supplier.get(), 1);
	}
}
