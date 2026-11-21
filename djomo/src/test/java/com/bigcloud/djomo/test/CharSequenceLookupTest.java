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

import com.bigcloud.djomo.internal.CharArraySequence;
import com.bigcloud.djomo.internal.CharSequenceLookup;

public class CharSequenceLookupTest {
	@Test
	public void testCharSequenceLookup() {
		List<String> testWords = List.of("id", "title", "type", "tide", "titleLang", "potato", "potash", "pota");
		CharSequenceLookup<String> lookup = new CharSequenceLookup<String>(
				testWords.stream().collect(Collectors.toMap(Function.identity(), Function.identity())));
		lookup.toString();
		testWords.forEach(word -> {
			Assert.assertEquals(lookup.get(word), word);
			CharArraySequence seq = new CharArraySequence(word.toCharArray());
			seq.start = 0; 
			seq.len = word.length();
			Assert.assertEquals(lookup.get(seq), word);
		});
		Assert.assertEquals(lookup.get("missing"), null);
		Assert.assertEquals(lookup.get("ti"), null);
		Assert.assertEquals(lookup.get("titl"), null);
		Assert.assertEquals(lookup.get("titla"), null);
	}

	@Test
	public void testEmptyLookup() {
		CharSequenceLookup<String> lookup = new CharSequenceLookup<String>(Map.of());
		Assert.assertEquals(lookup.get("missing"), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testBadLookupSet() {
		new CharSequenceLookup<String>(Map.of("\nbad", "test"));
	}

	@Test
	public void testBadLookupGet() {
		var result = new CharSequenceLookup<String>(Map.of("bad", "test", "test", "bad")).get("\nbad");
		Assert.assertNull(result);
	}

	@Test
	public void testQueryEndingOnChildSelectionIsMiss() {
		// "abde" keeps a non-empty prefix ("e") on the node reached by selecting
		// the 'd' child; a query that ends exactly on that selection must be a
		// miss, not a hit on the longer key's data
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(
				Map.of("ab", "v1", "abde", "v2"));
		Assert.assertEquals(lookup.get("ab"), "v1");
		Assert.assertEquals(lookup.get("abde"), "v2");
		Assert.assertEquals(lookup.get("abd"), null);
	}

	// ── Unicode keys ─────────────────────────────────────────────────────────
	//
	// Java identifiers may contain any unicode letter, so the lookup supports
	// keys with characters outside the 32-126 range by matching them against
	// their escape expansion; these tests exercise that, plus the ascii-miss
	// and control-character (null) semantics around it.

	@Test
	public void testUnicodeKeySetAndGet() {
		// non-ascii keys (é U+00E9, ï U+00EF) stored and looked up by string
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(
				Map.of("café", "v1", "naïve", "v2"));
		Assert.assertEquals(lookup.get("café"), "v1");
		Assert.assertEquals(lookup.get("naïve"), "v2");
		// ascii misses keep their existing null semantics
		Assert.assertEquals(lookup.get("caf"), null);
	}

	@Test
	public void testUnicodeGetReturnsNull() {
		// the stored key is a strict prefix of the query; the non-ascii char
		// selects a missing child, which is a plain miss
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(Map.of("caf", "test"));
		Assert.assertEquals(lookup.get("café"), null);
	}

	@Test
	public void testUnicodeAndAsciiMixedPrefix() {
		// the keys diverge at position 3 (é vs e); the é child-select currently
		// throws at construction for at least one insertion order
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(
				Map.of("café", "v1", "cafeteria", "v2"));
		Assert.assertEquals(lookup.get("café"), "v1");
		Assert.assertEquals(lookup.get("cafeteria"), "v2");
		Assert.assertEquals(lookup.get("cafe"), null);
		Assert.assertEquals(lookup.get("cafétéria"), null);
	}

	@Test
	public void testUnicodeCharArraySequence() {
		// the parse path passes a CharArraySequence over the raw buffer, so the
		// fast path of get() must handle non-ascii characters too
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(
				Map.of("caf", "caf", "café", "café"));
		for (String word : List.of("caf", "café")) {
			CharArraySequence seq = new CharArraySequence(word.toCharArray());
			seq.start = 0;
			seq.len = word.length();
			Assert.assertEquals(lookup.get(seq), word);
		}
		// a unicode extension of the stored prefix is a miss, not an IAE
		String missingWord = "café2";
		CharArraySequence missing = new CharArraySequence(missingWord.toCharArray());
		missing.start = 0;
		missing.len = missingWord.length();
		Assert.assertEquals(lookup.get(missing), null);
	}

	@Test
	public void testSupplementaryCharacterKey() {
		// U+1D573 (𝕳) is a supplementary codepoint and a legal java identifier
		// character; the ascii prefix "cl" makes the key reach a missing trie
		// branch before the non-bmp character, so a fix that only diverts on an
		// out-of-range char mid-traversal is not sufficient.
		// (java unicode escapes are 4 hex digits, so the codepoint is written
		// as an explicit surrogate pair)
		String key = "cl\uD835\uDF73ss";
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(Map.of(key, "v"));
		Assert.assertEquals(lookup.get(key), "v");
		CharArraySequence seq = new CharArraySequence(key.toCharArray());
		seq.start = 0;
		seq.len = key.length();
		Assert.assertEquals(lookup.get(seq), "v");
		Assert.assertEquals(lookup.get("cl\uD835\uDF3Ass"), null);
	}

	@Test
	public void testComposedAndDecomposedDiffer() {
		// lookup is an exact char-sequence comparison: a composed é (U+00E9)
		// must not match a decomposed "e" + combining acute (U+0301) query
		CharSequenceLookup<String> lookup = new CharSequenceLookup<>(Map.of("café", "v1"));
		Assert.assertEquals(lookup.get("cafe\u0301"), null);
	}
}
