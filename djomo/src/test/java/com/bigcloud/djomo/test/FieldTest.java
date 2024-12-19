/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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

import static org.testng.Assert.*;

import java.io.IOException;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;

public class FieldTest {
	public static class TypeA {
		protected String a;
		public String b;

		public String getA() {
			return a;
		}
	}

	public static class TypeB {
		protected String a;
		public String b;

		public void setA(String a) {
			this.a = a;
		}
	}

	Json json = new Json();

	@Test
	public void testFieldNoSetter() throws IOException {
		TypeA typeA = new TypeA();
		typeA.a = "A";
		typeA.b = "B";
		String str = json.toString(typeA);
		assertEquals(str, "{\"a\":\"A\",\"b\":\"B\"}");
		TypeA rt = json.fromString(str, TypeA.class);
		assertEquals(rt.a, null);
		assertEquals(rt.b, "B");
	}

	@Test
	public void testFieldNoGetter() throws IOException {
		TypeB typeB = new TypeB();
		typeB.a = "A";
		typeB.b = "B";
		String str = json.toString(typeB);
		assertEquals(str, "{\"b\":\"B\"}");
		TypeB rt = json.fromString("{\"a\":\"A\",\"b\":\"B\"}", TypeB.class);
		assertEquals(rt.a, "A");
		assertEquals(rt.b, "B");
	}
}
