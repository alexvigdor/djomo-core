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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Model;

public class OptionalTest {
	Json Json = new Json();
	@Test
	public void testOptional() throws IOException {
		String raw = "{\"d\":0.12345,\"i\":12345,\"l\":12345167890,\"number\":1,\"other\":{\"name\":\"foo\"}}";
		Something something = Json.fromString(raw, Something.class);
		Assert.assertEquals(something.number.get(), (Integer) 1);
		Assert.assertEquals(something.other.get().name.get(), "foo");
		Assert.assertEquals(something.i.getAsInt(), 12345);
		Assert.assertEquals(something.l.getAsLong(), 12345167890l);
		Assert.assertEquals(something.d.getAsDouble(), 0.12345);
		String rt = Json.toString(something);
		Assert.assertEquals(rt, raw);
		raw = "{\"d\":null,\"i\":null,\"l\":null,\"number\":1,\"other\":{\"name\":null}}";
		something = Json.fromString(raw, Something.class);
		Assert.assertEquals(something.number.get(), (Integer) 1);
		Assert.assertEquals(something.other.get().name.isEmpty(), true);
		Assert.assertEquals(something.i.isEmpty(), true);
		Assert.assertEquals(something.l.isEmpty(), true);
		Assert.assertEquals(something.d.isEmpty(), true);
		rt = Json.toString(something);
		Assert.assertEquals(rt, raw);
		Model<OptionalInt> oim = Json.models().get(OptionalInt.class);
		Assert.assertEquals(oim.convert(null), OptionalInt.empty());
		Assert.assertEquals(oim.convert("123"), OptionalInt.of(123));
		Model<OptionalLong> olm = Json.models().get(OptionalLong.class);
		Assert.assertEquals(olm.convert(null), OptionalLong.empty());
		Assert.assertEquals(olm.convert("12345167890"), OptionalLong.of(12345167890l));
		Model<OptionalDouble> odm = Json.models().get(OptionalDouble.class);
		Assert.assertEquals(odm.convert(null), OptionalDouble.empty());
		Assert.assertEquals(odm.convert("0.123456789"), OptionalDouble.of(0.123456789));
		Model<Optional<Float>> ofm = Json.models().get(new StaticType<Optional<Float>>() {});
		Assert.assertEquals(ofm.convert(null), Optional.empty());
		Assert.assertEquals(ofm.convert("0.123456789"), Optional.of(Float.valueOf(0.123456789f)));
	}

	public static class Something {
		public OptionalDouble d;
		public OptionalInt i;
		public OptionalLong l;
		public Optional<Integer> number;
		public Optional<Other> other;		
	}

	public static class Other {
		public Optional<String> name;
	}
}
