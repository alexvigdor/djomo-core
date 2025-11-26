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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.simple.ByteArrayBasedModel;

public class BigTest {
	
	@Test
	public void testNormalBig() throws IOException{
		Big big = new Big(new BigInteger("12345678901234567890"),
				new BigDecimal("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890.0"));
		Json json = new Json();
		String str = json.toString(big);
		Big rt = json.fromString(str, Big.class);
		Assert.assertEquals(rt, big);
	}
	
	@Test
	public void testBinaryBig() throws IOException{
		Big big = new Big(new BigInteger("12345678901234567890"),
				new BigDecimal("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890.0"));
		Json json = new Json(Models.builder().factory((type, context)->{
			var lookup = MethodHandles.lookup();
			try {
				if(type == BigInteger.class) {
					return new ByteArrayBasedModel<>(type, context, lookup.findConstructor(BigInteger.class, MethodType.methodType(void.class, byte[].class)), lookup.unreflect(BigInteger.class.getMethod("toByteArray")));
				}
			}
			catch(NoSuchMethodException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			return null;
		}).build());
		String str = json.toString(big);
		Big rt = json.fromString(str, Big.class);
		Assert.assertEquals(rt, big);
		byte[] bytes = rt.bi.toByteArray();
		BigInteger cv = json.models().get(BigInteger.class).convert(Base64.getEncoder().encodeToString(bytes));
		Assert.assertEquals(cv, rt.bi);
	}
	
	@Test
	public void testUnquotedBig() throws IOException {
		String source = "{\"bd\":123456789123456789.0123456789123456789, \"bi\": 123456789123456789123456789123456789}";
		Json json = new Json();
		var parsed = json.fromString(source);
		var big = json.fromString(source, Big.class);
		StringBuilder overflowBuilder = new StringBuilder("[");
		for(int i = 0; i < 250; i++) {
			if(i > 0) {
				overflowBuilder.append(",");
			}
			overflowBuilder.append("123456789123456789.0123456789123456789");
		}
		overflowBuilder.append("]");
		List<BigDecimal> result = json.fromString(overflowBuilder.toString(), new StaticType<List<BigDecimal>>() {});
		result.forEach(bd -> Assert.assertEquals(bd, big.bd));
	}
 	
	public static record Big(
			BigInteger bi,
			BigDecimal bd
			) {}
}
