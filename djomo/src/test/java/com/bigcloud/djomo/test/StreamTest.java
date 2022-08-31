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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;

public class StreamTest {
	Models Models = new Models();
	Json Json = new Json(Models);
	@Test
	public void testStream() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String value="0XaQǨ#@ʩ$-ω[ҹ+ש!᪘≉=⪷⽙~㹚哸'𐃢🇭🇳";
		ListMaker<List<String>, String> maker = (ListMaker<List<String>, String>) ((ListModel)Models.get(new StaticType<ArrayList<String>>() {})).maker();
		for(int i=0; i<1000;i++) {
			maker.item(value);
		}
		var orig = maker.make();
		Json.write(orig, baos);
		//System.out.println(new String(baos.toByteArray(), "UTF-8"));
		var rt = Json.read(new ByteArrayInputStream(baos.toByteArray()), new StaticType<List<String>>() {
		});
		Assert.assertEquals(rt, orig);
	}
}
