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
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;

public class RecordTest {
	Models Models = new Models();
	Json Json = new Json(Models);
	@Test
	public void testRecords() throws IOException {
		Envelope envelope = new Envelope(13, "you", new Message("Testing", "Are you there?"));
		String json = Json.toString(envelope);
		Envelope roundTrip = Json.fromString(json, Envelope.class);
		Assert.assertEquals(roundTrip, envelope);
		envelope = new Envelope(888, "me", new Message("Made it", "In the shade"));
		json = Json.toString(envelope);
		roundTrip = Json.fromString(json, Envelope.class);
		Assert.assertEquals(roundTrip, envelope);
	}

	@Test
	public <F extends Field<Envelope,?,Object>> void testMaker() {
		Envelope envelope = new Envelope(13, "you", new Message("Testing", "Are you there?"));
		var em = ((ObjectModel<Envelope,ObjectMaker<Envelope,F,Object>,F, ?, Object>)Models.get(Envelope.class));
		var maker = em.maker(envelope);
		maker.field(em.getField("id"), 99);
		envelope = maker.make();
		Assert.assertEquals(envelope.id(), 99);
		Assert.assertEquals(envelope.to(), "you");
	}

	@Test(expectedExceptions = RuntimeException.class)
	public void testBadRecord() throws IOException {
		((ObjectModel)Models.get(Envelope.class)).maker().make();
	}

	@Order({ "id", "to" })
	public static record Envelope(int id, String to, Message message) {
	};

	@Order({ "subject", "body" })
	public static record Message(String subject, String body) {
	};
}
