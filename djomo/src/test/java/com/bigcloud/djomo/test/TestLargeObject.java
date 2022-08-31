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
import java.util.HashMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;

public class TestLargeObject {
	@Test
	public void testLargeObject() throws IOException {
		Json Json = new Json();
		var map = new HashMap();
		for (int i = 1; i < 100; i++) {
			var sub = new HashMap<>();
			for (int j = 1; j < 100; j++) {
				char[] chars = new char[j * i];
				Arrays.fill(chars, 'a');
				sub.put(String.valueOf(j), new String(chars));
				sub.put("foo", "\" \\ \"");
			}
			map.put(String.valueOf(i), sub);
		}
		var rt = Json.fromString(Json.toString(map));
		Assert.assertEquals(rt, map);
	}
}
