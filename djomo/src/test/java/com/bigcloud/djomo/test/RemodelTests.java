/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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
import java.lang.reflect.Type;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.annotation.Remodel;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class RemodelTests {
	Json json = new Json();

	@Remodel(CustomRepModel.class)
	public static class CustomRep {
		public String a;
		public String b;
	}

	public static class CustomRepModel extends BaseModel<CustomRep> {

		public CustomRepModel(Type type, ModelContext context) {
			super(type, context);
		}

		@Override
		public CustomRep parse(Parser parser) {
			String str = parser.parseString().toString();
			var parts = str.split(":");
			CustomRep cr = new CustomRep();
			cr.a = parts[0];
			cr.b = parts[1];
			return cr;
		}

		@Override
		public void visit(CustomRep obj, Visitor visitor) {
			visitor.visitString(obj.a.concat(":").concat(obj.b));
		}

	}

	@Test
	public void testRemodel() throws IOException {
		CustomRep cr = new CustomRep();
		cr.a = "foo";
		cr.b = "bar";
		String ser = json.toString(cr);
		Assert.assertEquals(ser, "\"foo:bar\"");
		CustomRep rt = json.fromString(ser, CustomRep.class);
		Assert.assertEquals(rt.a, cr.a);
		Assert.assertEquals(rt.b, cr.b);
	}

	@Test
	public void testOverride() throws IOException {
		Json ojson = new Json(Models.builder()
				.factory(CustomRep.class, (type, context) -> new BaseModel<CustomRep>(type, context) {

					@Override
					public CustomRep parse(Parser parser) {
						String str = parser.parseString().toString();
						var parts = str.split(",");
						CustomRep cr = new CustomRep();
						cr.b = parts[0];
						cr.a = parts[1];
						return cr;
					}

					@Override
					public void visit(CustomRep obj, Visitor visitor) {
						visitor.visitString(obj.b.concat(",").concat(obj.a));
					}

				})
				.build());
		CustomRep cr = new CustomRep();
		cr.a = "foo";
		cr.b = "bar";
		String ser = ojson.toString(cr);
		Assert.assertEquals(ser, "\"bar,foo\"");
		CustomRep rt = ojson.fromString(ser, CustomRep.class);
		Assert.assertEquals(rt.a, cr.a);
		Assert.assertEquals(rt.b, cr.b);
	}
}
