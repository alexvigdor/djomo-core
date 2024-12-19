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

import org.testng.annotations.Test;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.test.ComplexModel.Direction;

public class ComplexModelTest {
	Models models = new Models();
	@Test
	public void testComplexModel() {
		Map<?, ?> testData = Map.of(
				"direction", "NORTH",
				"history", Map.of(
						"SOUTH", List.of("12345", "67890"),
						"EAST", List.of("12345678912345", "98765432198765")),
				"models", List.of(
						Map.of(
								"name", "hello",
								"count", "99999",
								"enabled", "true"),
						Map.of(
								"name", "world",
								"count", "33333",
								"enabled", "false")),
				"children", Map.of(
						"junior", Map.of("nick", "jr"),
						"senior", Map.of("nick", "sr")));
		ComplexModel result = (ComplexModel) models.get(ComplexModel.class).convert(testData);
		assertEquals(result.getDirection(), Direction.NORTH);
		assertEquals(result.getHistory().size(), 2);
		assertEquals(result.getHistory().get(Direction.EAST).get(1).longValue(), 98765432198765l);
		assertEquals(result.getModels().get(0).getName(), "hello");
		assertEquals(result.getModels().get(1).getCount(), 33333l);
		assertEquals(result.getChildren().get("junior").get("nick"), "jr");
		assertEquals(result.getChildren().get("senior").get("nick"), "sr");
	}
}
