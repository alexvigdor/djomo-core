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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.internal.CharArrayLookup;
import com.bigcloud.djomo.internal.CharArraySequence;

public class CharArrayLookupTest {
	@Test
	public void testCharArrayLookup() {
		List<String> testWords = List.of("id", "title", "type", "tide", "titleLang", "potato", "potash", "pota");
		CharArrayLookup<String> lookup = new CharArrayLookup<String>(
				testWords.stream().collect(Collectors.toMap(Function.identity(), Function.identity())));
		lookup.toString();
		testWords.forEach(word -> {
			Assert.assertEquals(lookup.get(word), word);
			Assert.assertEquals(lookup.get(new CharArraySequence(word.toCharArray(), 0, word.length())), word);
		});
		Assert.assertEquals(lookup.get("missing"), null);
		Assert.assertEquals(lookup.get("ti"), null);
		Assert.assertEquals(lookup.get("titl"), null);
		Assert.assertEquals(lookup.get("titla"), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testBadLookupSet() {
		new CharArrayLookup<String>(Map.of("\nbad", "test"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testBadLookupGet() {
		new CharArrayLookup<String>(Map.of("bad", "test", "test", "bad")).get("\nbad");
	}
}
