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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Model;

public class AtomicTest {
	
	@Test
	public void testAtomic() throws IOException {
		Atomize at = new Atomize(new AtomicBoolean(true), new AtomicInteger(123), new AtomicLong(123456789012345l), new AtomicReference<>("Atomized"));
		Json json = new Json();
		String str = json.toString(at);
		Assert.assertEquals(str, "{\"bool\":true,\"integ\":123,\"lng\":123456789012345,\"ref\":\"Atomized\"}");
		Atomize rt = json.fromString(str, Atomize.class);
		Assert.assertEquals(rt.bool.get(), at.bool.get());
		Assert.assertEquals(rt.integ.get(), at.integ.get());
		Assert.assertEquals(rt.lng.get(), at.lng.get());
		Assert.assertEquals(rt.ref.get(), at.ref.get());
	}
	
	@Test
	public void testConvert() {
		Models models = new Models();
		var aim = models.get(AtomicInteger.class);
		Assert.assertEquals(aim.convert("1").get(), 1);
		var alm = models.get(AtomicLong.class);
		Assert.assertEquals(alm.convert("12345678901").get(), 12345678901l);
		var abm = models.get(AtomicBoolean.class);
		Assert.assertEquals(abm.convert("true").get(), true);
		Model<AtomicReference<Double>> arm = models.get(new StaticType<AtomicReference<Double>>() {});
		Assert.assertEquals(arm.convert("1.23").get(), 1.23);
	}
	
	public static record Atomize (
		 AtomicBoolean bool,
		 AtomicInteger integ,
		 AtomicLong lng,
		 AtomicReference<String> ref
	) {}
}
