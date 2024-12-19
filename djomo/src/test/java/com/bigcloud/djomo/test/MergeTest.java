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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.filter.parsers.ExcludeParser;
import com.bigcloud.djomo.filter.parsers.OmitNullItemParser;
import com.bigcloud.djomo.filter.visitors.OmitNullFieldVisitor;
import com.bigcloud.djomo.test.ComplexModel.Direction;

public class MergeTest {
	Json Json = new Json();
	@Test
	public void testMerge() throws IOException {
		ComplexModel ma = ComplexModel.builder()
				.models(List.of(
						ImmutableModel.builder().name("foo").enabled(true).build(),
						ImmutableModel.builder().name("bar").count(4).build()
						)
				)
				.direction(Direction.WEST)
				.history(new EnumMap(Map.of(ComplexModel.Direction.NORTH, List.of(1l,2l,3l), ComplexModel.Direction.EAST,  List.of(4l,5l,6l))))
				.build();
		ComplexModel mb = ComplexModel.builder()
				.models(Arrays.asList(
						null,
						ImmutableModel.builder().name("update").build()))
				.history(new EnumMap(Map.of(ComplexModel.Direction.SOUTH, List.of(-1l,-2l,-3l), ComplexModel.Direction.EAST,  List.of(-4l,-5l))))
				.build();
		
		String mj = Json.toString(mb, new OmitNullFieldVisitor());
		ComplexModel merged = Json.fromString(mj, ma);
		Assert.assertEquals(Json.toString(merged), "{\"children\":null,\"direction\":\"WEST\",\"history\":{\"NORTH\":[1,2,3],\"SOUTH\":[-1,-2,-3],\"EAST\":[4,5,6,-4,-5]},\"models\":[{\"count\":0,\"enabled\":true,\"name\":\"foo\"},{\"count\":4,\"enabled\":false,\"name\":\"bar\"},null,{\"count\":0,\"enabled\":false,\"name\":\"update\"}]}");
	}
	
	@Test
	public void testMergeFilter() throws IOException {
		ComplexModel ma = ComplexModel.builder()
				.models(List.of(
						ImmutableModel.builder().name("foo").enabled(true).build(),
						ImmutableModel.builder().name("bar").count(4).build())
				)
				.direction(Direction.WEST)
				.history(new EnumMap(Map.of(ComplexModel.Direction.NORTH, List.of(1l,2l,3l), ComplexModel.Direction.EAST,  List.of(4l,5l,6l))))
				.build();
		ComplexModel mb = ComplexModel.builder()
				.models(Arrays.asList(
						null,
						ImmutableModel.builder().name("update").build()))
				.history(new EnumMap(Map.of(ComplexModel.Direction.SOUTH, List.of(-1l,-2l,-3l), ComplexModel.Direction.EAST,  List.of(-4l,-5l))))
				.build();
		
		String mj = Json.toString(mb, new OmitNullFieldVisitor());
		ComplexModel merged = Json.fromString(mj, ma, new OmitNullItemParser(), new ExcludeParser(ComplexModel.class, "history"));
		Assert.assertEquals(Json.toString(merged), "{\"children\":null,\"direction\":\"WEST\",\"history\":{\"NORTH\":[1,2,3],\"EAST\":[4,5,6]},\"models\":[{\"count\":0,\"enabled\":true,\"name\":\"foo\"},{\"count\":4,\"enabled\":false,\"name\":\"bar\"},{\"count\":0,\"enabled\":false,\"name\":\"update\"}]}");
	}
}
