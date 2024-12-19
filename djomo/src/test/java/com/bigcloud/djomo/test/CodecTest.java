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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.filter.ObjectFieldListCodec;
import com.bigcloud.djomo.filter.parsers.IncludeParser;
import com.bigcloud.djomo.filter.visitors.IncludeVisitor;
import com.bigcloud.djomo.filter.visitors.ObjectFieldListVisitor;

public class CodecTest {
	
	@Test
	public void testObjectFieldCodec() throws IOException {
		Json json = Json.builder().scan(ObjectFieldListCodec.class).build();
		Contact sample = sampleContact();
		String str = json.toString(sample);
		Contact rt  = json.fromString(str, Contact.class);
		Assert.assertEquals(rt, sample);
	}
	
	@Test
	public void testObjectFieldCodecWithIncludes() throws IOException {
		Models models = new Models();
		Json json = Json.builder()
				.models(models)
				.visit(new IncludeVisitor<>(Contact.class, "nickName", "phoneNumbers"))
				.visit(new IncludeVisitor<>(EnumMap.class, "work"))
				.scan(ObjectFieldListCodec.class)
				.parse(new IncludeParser<>(Contact.class, "nickName", "phoneNumbers"))
				.parse(new IncludeParser<>(EnumMap.class, "work"))
				.build();
		Contact sample = sampleContact();
		String str = json.toString(sample);
		Contact rt  = json.fromString(str, Contact.class);
		Assert.assertNotEquals(rt, sample);
		Assert.assertEquals(rt.nickName, sample.nickName);
		Assert.assertEquals(rt.fullName, null);
		Assert.assertEquals(rt.phoneNumbers.get(Device.work), sample.phoneNumbers.get(Device.work));
		Assert.assertEquals(rt.phoneNumbers.get(Device.home), null);
	}
	
	@Test
	public void testObjectFieldConvert() {
		ObjectFieldListVisitor visitor = new ObjectFieldListVisitor();
		Json json = new Json();
		Models models = new Models();
		List converted = models.listModel.convert(sampleContact());
	}

	private Contact sampleContact() {
		EnumMap<Device, PhoneNumber> numbers = new EnumMap<>(Device.class);
		numbers.put(Device.mobile, new PhoneNumber(1,646,9876543,0));
		numbers.put(Device.home, new PhoneNumber(1,718,1234567,0));
		numbers.put(Device.work, new PhoneNumber(1,212,9183746,555));
		return new Contact(
				"Joe Schmoe",
				"Joe",
				new Address(9876, "Wormhole Way", "Townville"),
				numbers,
				Map.of("bedtime","9:00 on weekdays","diet","vegan")
		);
	}
	
	public static record Contact (
		String fullName,
		String nickName,
		Address homeAddress,
		EnumMap<Device, PhoneNumber> phoneNumbers,
		Map<String, String> notes
	) {}
	
	public static enum Device {
		mobile,
		home,
		work
	}
	
	public static record Address (
		int streetNumber,
		String steetName,
		String cityName
	) {}
	
	public static record PhoneNumber(
			int countryCode,
			int areaCode,
			int localPart,
			int extension
	) {}
}
