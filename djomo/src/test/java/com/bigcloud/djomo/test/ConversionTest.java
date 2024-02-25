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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;

public class ConversionTest {
	Models Models = new Models();
	@Test
	public void testConvertMapToBean() {
		Map source = Map.of("name", "foo", "count", "123", "enabled", "true");
		ImmutableModel dest = Models.get(ImmutableModel.class).convert(source);
		assertEquals(dest.getName(), "foo");
		assertEquals(dest.getCount(), 123);
		assertEquals(dest.isEnabled(), true);
	}
	@Test
	public void testConvertBeanToMap() {
		ImmutableModel source =ImmutableModel.builder().name("foo").count(123).enabled(true).build();
		Map dest = Models.get(Map.class).convert(source);
		assertEquals(dest.get("name"), "foo");
		assertEquals(dest.get("count"), 123);
		assertEquals(dest.get("enabled"), true);
	}
	@Test
	public void testConvertListToArray() {
		List<String> test = List.of("a","b","c");
		String[] strings = Models.get(String[].class).convert(test);
		Assert.assertEquals(String.join("",strings), "abc");
	}

	@Test
	public void testConvertArrayToList() {
		String[] test = new String[] {"a","b","c"};
		List<String> strings = (List<String>) Models.get(new StaticType<List<String>>() {}).convert(test);
		Assert.assertEquals(strings.stream().collect(Collectors.joining()), "abc");
	}
//
//	@Test(expectedExceptions = RuntimeException.class)
//	public void testStrictConvertFail() {
//		var model =  ((ObjectModel<ImmutableModel>)Models.get(ImmutableModel.class));
//		ObjectMaker<ImmutableModel> maker =model.maker();
//		maker.setField(model.getField("name"),  "foo");
//		maker.setField(model.getField("count"),  "123");
//		maker.setField(model.getField("enabled"),  "true");
//	}
}
