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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.annotation.Ignore;

import lombok.Builder;
import lombok.Value;

public class IgnoreTests {
	Json json = new Json();

	public static class PublicFieldClass {
		public String foo;
		@Ignore
		public String bar;
	}

	public static class BeanPrivatePropertyClass {
		private String foo;
		@Ignore
		private String bar;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@Test
	public void testBeanPrivateProperty() throws IOException {
		BeanPrivatePropertyClass pfc = new BeanPrivatePropertyClass();
		pfc.setFoo("abc");
		pfc.setBar("def");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo\":\"abc\",\"bar\":\"def\"}", BeanPrivatePropertyClass.class);
		Assert.assertNull(rt.getBar());
	}

	@Test
	public void testPublicFieldProperty() throws IOException {
		PublicFieldClass pfc = new PublicFieldClass();
		pfc.foo = "abc";
		pfc.bar = "def";
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo\":\"abc\",\"bar\":\"def\"}", PublicFieldClass.class);
		Assert.assertNull(rt.bar);
	}

	public static class BeanGetterClass {
		private String foo;
		private String bar;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		@Ignore
		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@Test
	public void testBeanGetterProperty() throws IOException {
		BeanGetterClass pfc = new BeanGetterClass();
		pfc.setFoo("abc");
		pfc.setBar("def");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo\":\"abc\",\"bar\":\"def\"}", BeanGetterClass.class);
		Assert.assertNull(rt.bar);
	}

	public static class BeanSetterClass {
		private String foo;
		private String bar;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return bar;
		}

		@Ignore
		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@Test
	public void testBeanSetterProperty() throws IOException {
		BeanSetterClass pfc = new BeanSetterClass();
		pfc.setFoo("abc");
		pfc.setBar("def");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo\":\"abc\",\"bar\":\"def\"}", BeanSetterClass.class);
		Assert.assertNull(rt.bar);
	}

	public static record FooRecord(String foo, @Ignore String bar) {
	};

	@Test
	public void testRecordProperty() throws IOException {
		FooRecord pfc = new FooRecord("abc", "def");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo\":\"abc\",\"bar\":\"def\"}", FooRecord.class);
		Assert.assertNull(rt.bar());
	}

	@Value
	@Builder
	public static class BuilderClass {
		String foo;
		@Ignore
		String bar;
	}

	@Test
	public void testBuilderClass() throws IOException {
		BuilderClass pfc = BuilderClass.builder().foo("abc").bar("def").build();
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo\":\"abc\",\"bar\":\"def\"}", BuilderClass.class);
		Assert.assertNull(rt.getBar());
	}
}
