package com.bigcloud.djomo.test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.internal.CharArrayLookup;
import com.bigcloud.djomo.internal.CharArraySequence;

public class CharArrayLookupTest {
	@Test
	public void testCharArrayLookup() {
		List<String> testWords = List.of("id","title","type","tide","titleLang","potato","potash","pota");
		CharArrayLookup<String> lookup = new CharArrayLookup<String>(testWords.stream().collect(Collectors.toMap(Function.identity(), Function.identity())));
		System.out.println("Lookup "+lookup);
		testWords.forEach(word->{
			Assert.assertEquals(lookup.get(word), word);
			Assert.assertEquals(lookup.get(new CharArraySequence(word.toCharArray(), 0, word.length())), word);
		});
		Assert.assertEquals(lookup.get("missing"), null);
		Assert.assertEquals(lookup.get("ti"), null);
		Assert.assertEquals(lookup.get("titl"), null);
		Assert.assertEquals(lookup.get("titla"), null);
	}
}
