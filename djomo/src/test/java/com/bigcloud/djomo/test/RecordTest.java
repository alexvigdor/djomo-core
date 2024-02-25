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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.annotation.Order;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.InstanceParser;

public class RecordTest {
	Models models = new Models();
	Json json = new Json(models);

	@Test
	public void testRecords() throws IOException {
		Envelope envelope = new Envelope(13, "you", new Message("Testing", "Are you there?"));
		String out = json.toString(envelope);
		Envelope roundTrip = json.fromString(out, Envelope.class);
		Assert.assertEquals(roundTrip, envelope);
		envelope = new Envelope(888, "me", new Message("Made it", "In the shade"));
		out = json.toString(envelope);
		roundTrip = json.fromString(out, Envelope.class);
		Assert.assertEquals(roundTrip, envelope);
	}

	@Test
	public void testMaker() {
		Envelope envelope = new Envelope(13, "you", new Message("Testing", "Are you there?"));
		ObjectModel<Envelope> em = ((ObjectModel<Envelope>) models.get(Envelope.class));
		var maker = em.maker(envelope);
		em.getField("id").parse(maker, new InstanceParser(models, 99));
		envelope = em.make(maker);
		Assert.assertEquals(envelope.id(), 99);
		Assert.assertEquals(envelope.to(), "you");
	}

	@Test(expectedExceptions = RuntimeException.class)
	public void testBadRecord() throws IOException {
		ObjectModel model =(ObjectModel) models.get(Envelope.class); 
		model.make(model.maker());
	}

	@Order({ "id", "to" })
	public static record Envelope(int id, String to, Message message) {
	};

	@Order({ "subject", "body" })
	public static record Message(String subject, String body) {
	};

	@Test
	public void testVirtualField() {
		Extended extended = new Extended(123, "Hello World");
		String out = json.toString(extended);
		Assert.assertEquals(out, "{\"id\":123,\"name\":\"Hello World\",\"nameLength\":11}");
	}

	public static record Extended(int id, String name) {
		public int getNameLength() {
			return name == null ? 0 : name.length();
		}
	}
}
