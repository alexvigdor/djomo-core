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
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.io.Buffer;
import com.bigcloud.djomo.json.JsonParser;

import lombok.Builder;
import lombok.Value;

public class PrimitiveTest {
	Json Json = new Json();
	@Test
	public void testPrimitives() throws IOException {
		Primitives ps = Primitives.builder()
				.by(Byte.MAX_VALUE)
				.sh(Short.MIN_VALUE)
				.in(Integer.MAX_VALUE)
				.lo(Long.MAX_VALUE)
				.fl(3e-38f)
				.du(1e308)
				.bo(Boolean.TRUE)
				.ch('C')
				.build();
		String json = Json.toString(ps);
		Primitives rt = Json.fromString(json, Primitives.class);
		Assert.assertEquals(rt, ps);
	}

	@Test
	public void testPrimitiveWrappers() throws IOException {
		PrimitiveWrappers ps = PrimitiveWrappers.builder()
				.by(Byte.MAX_VALUE)
				.sh(Short.MAX_VALUE)
				.in(Integer.MAX_VALUE)
				.lo(Long.MAX_VALUE)
				.fl(3e38f)
				.du(1e+308)
				.bo(Boolean.TRUE)
				.ch('C')
				.build();
		String json = Json.toString(ps);
		PrimitiveWrappers rt = Json.fromString(json, PrimitiveWrappers.class);
		Assert.assertEquals(rt, ps);
	}

	@Test
	public void testNegativePrimitiveWrappers() throws IOException {
		PrimitiveWrappers ps = PrimitiveWrappers.builder()
				.by(Byte.MIN_VALUE)
				.sh(Short.MIN_VALUE)
				.in(Integer.MIN_VALUE)
				.lo(Long.MIN_VALUE)
				.fl(-Float.MAX_VALUE)
				.du(-Double.MAX_VALUE)
				.bo(Boolean.FALSE)
				.ch('C')
				.build();
		String json = Json.toString(ps);
		PrimitiveWrappers rt = Json.fromString(json, PrimitiveWrappers.class);
		Assert.assertEquals(rt, ps);
	}

	@Test
	public void testNumberBuffer() throws IOException {
		Models models = new Models();
		Map<String, Number> samples = Map.of("-387.2498e+13", -387.2498e+13, "-387.2498e+0", -387.2498, "-387.2", -387.2, "-387.", -387.0, "-3", -3.0);
		samples.forEach((str, num) -> {
				Double val = new JsonParser(models, new Buffer(new char[7], new StringReader(str)), new Buffer(new char[26])).parseDouble();
				Assert.assertEquals(val, num);
		});
		Model<List<Double>> listDoubleModel = models.get(new StaticType<List<Double>>() {});
		for (int bufsize : new int[] { 9, 11, 13, 15 }) {
			samples.forEach((str, num) -> {
				List<Double> val =
						(List<Double>) new JsonParser(models, new Buffer(new char[bufsize], new StringReader("[" + str + "]")), new Buffer(new char[26])).parse(listDoubleModel);
				Assert.assertEquals(val.get(0), num);
			});
		}
	}

	@Test(expectedExceptions = NumberFormatException.class)
	public void testBadNumber() throws IOException {
		Double val = new JsonParser(new Models(), new Buffer(new char[3], new StringReader("-1..0")), new Buffer(new char[26])).parseDouble();
	}

	@Test(expectedExceptions = NumberFormatException.class)
	public void testEmptyNumber() throws IOException {
		Double val = new JsonParser(new Models(), new Buffer(new char[1], new StringReader("")), new Buffer(new char[26])).parseDouble();
	}

	@Test(expectedExceptions = NumberFormatException.class)
	public void testBadDoubleString() throws IOException {
		new Json().fromString("-1..0", double.class);
	}

	@Test(expectedExceptions = NumberFormatException.class)
	public void testBadNumberString() throws IOException {
		new Json().fromString("-1..0", Number.class);
	}

	@Test
	public void testNumberString() throws IOException {
		Object val = new Json().fromString("1.234e-0", Number.class);
		Assert.assertEquals(val, 1.234);
	}

	@Test
	public void testPrimitiveStreams() {
		Models models = new Models();
		Model<Stream> streamModel = models.get(Stream.class);
		double[] ddata = { 1.0, 2.0, 3.0 };
		double dsum = (double) streamModel.convert(ddata).collect(Collectors.summingDouble(d -> (double) d));
		Assert.assertEquals(dsum, 6);
		long[] ldata = { 4, 5, 6 };
		long lsum = (long) streamModel.convert(ldata).collect(Collectors.summingLong(d -> (long) d));
		Assert.assertEquals(lsum, 15);
		int[] idata = { 7, 8, 9 };
		int isum = (int) streamModel.convert(idata).collect(Collectors.summingInt(d -> (int) d));
		Assert.assertEquals(isum, 24);
	}

	@Builder
	@Value
	public static class Primitives {
		byte by;
		short sh;
		int in;
		long lo;
		float fl;
		double du;
		boolean bo;
		char ch;
	}

	@Builder
	@Value
	public static class PrimitiveWrappers {
		Byte by;
		Short sh;
		Integer in;
		Long lo;
		Float fl;
		Double du;
		Boolean bo;
		Character ch;
	}
}
