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
import com.bigcloud.djomo.annotation.Property;
import com.bigcloud.djomo.error.AnnotationException;

import lombok.Builder;
import lombok.Value;

public class PropertyTests {
	Json json = new Json();

	public static class PublicFieldClass {
		@Property("foo-bar")
		public String foo;
	}

	public static class BeanPrivatePropertyClass {
		@Property("foo-bar")
		private String foo;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanPrivateProperty() throws IOException {
		BeanPrivatePropertyClass pfc = new BeanPrivatePropertyClass();
		pfc.setFoo("abc");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, BeanPrivatePropertyClass.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
	}

	@Test
	public void testPublicFieldProperty() throws IOException {
		PublicFieldClass pfc = new PublicFieldClass();
		pfc.foo = "abc";
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, PublicFieldClass.class);
		Assert.assertEquals(rt.foo, pfc.foo);
	}

	public static class BeanGetterClass {
		private String foo;

		@Property("foo-bar")
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanGetterProperty() throws IOException {
		BeanGetterClass pfc = new BeanGetterClass();
		pfc.setFoo("abc");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, BeanGetterClass.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
	}

	public static class BeanSetterClass {
		private String foo;

		public String getFoo() {
			return foo;
		}

		@Property("foo-bar")
		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanSetterProperty() throws IOException {
		BeanSetterClass pfc = new BeanSetterClass();
		pfc.setFoo("abc");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, BeanSetterClass.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
	}

	public static class BeanDoubleDefClass {
		private String foo;

		@Property("foo-bar")
		public String getFoo() {
			return foo;
		}

		@Property("foo-bar")
		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanDoubleDefProperty() throws IOException {
		BeanDoubleDefClass pfc = new BeanDoubleDefClass();
		pfc.setFoo("abc");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, BeanDoubleDefClass.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
	}

	public static class BeanBadDefClass {
		private String foo;

		@Property("fool-bar")
		public String getFoo() {
			return foo;
		}

		@Property("foo-bar")
		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanBadDefProperty() throws IOException {
		BeanBadDefClass pfc = new BeanBadDefClass();
		pfc.setFoo("abc");
		Assert.assertThrows(AnnotationException.class, () -> json.toString(pfc));
	}

	public static class BeanDupeDefClass {
		private String foo;
		public String bar;

		@Property("bar")
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanDupeDefProperty() throws IOException {
		BeanDupeDefClass pfc = new BeanDupeDefClass();
		pfc.setFoo("abc");
		Assert.assertThrows(AnnotationException.class, () -> json.toString(pfc));
	}

	public static class BeanBadAliasClass {
		private String foo;
		public String bar;

		@Property(alias = { "bar" })
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testBeanBadAliasProperty() throws IOException {
		BeanBadAliasClass pfc = new BeanBadAliasClass();
		pfc.setFoo("abc");
		Assert.assertThrows(AnnotationException.class, () -> json.toString(pfc));
	}

	public static record FooRecord(@Property("foo-bar") String foo) {
	};

	@Test
	public void testRecordProperty() throws IOException {
		FooRecord pfc = new FooRecord("abc");
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, FooRecord.class);
		Assert.assertEquals(rt.foo(), pfc.foo());
	}

	@Value
	@Builder
	public static class BuilderClass {
		@Property("foo-bar")
		String foo;
	}

	@Test
	public void testBuilderClass() throws IOException {
		BuilderClass pfc = BuilderClass.builder().foo("abc").build();
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo-bar\":\"abc\"}");
		var rt = json.fromString(str, BuilderClass.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
	}

	@Value
	@Builder
	public static class AliasBuilder {
		@Property(alias = "foo-bar")
		String foo;
	}

	@Test
	public void testAliasBuilder() throws IOException {
		AliasBuilder pfc = AliasBuilder.builder().foo("abc").build();
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"foo\":\"abc\"}");
		var rt = json.fromString("{\"foo-bar\":\"abc\"}", AliasBuilder.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
		var rt2 = json.fromString(str, AliasBuilder.class);
		Assert.assertEquals(rt2.getFoo(), pfc.getFoo());
	}

	@Value
	@Builder
	public static class NameAliasBuilder {
		@Property(value = "bar", alias = { "foo-bar", "roo-car" })
		String foo;
	}

	@Test
	public void testNameAliasBuilder() throws IOException {
		NameAliasBuilder pfc = NameAliasBuilder.builder().foo("abc").build();
		String str = json.toString(pfc);
		Assert.assertEquals(str, "{\"bar\":\"abc\"}");
		var rt = json.fromString("{\"foo-bar\":\"abc\"}", NameAliasBuilder.class);
		Assert.assertEquals(rt.getFoo(), pfc.getFoo());
		var rt2 = json.fromString("{\"roo-car\":\"abc\"}", NameAliasBuilder.class);
		Assert.assertEquals(rt2.getFoo(), pfc.getFoo());
		var rt3 = json.fromString(str, NameAliasBuilder.class);
		Assert.assertEquals(rt3.getFoo(), pfc.getFoo());
		var rt4 = json.fromString("{\"foo\":\"abc\"}", NameAliasBuilder.class);
		Assert.assertEquals(rt4.getFoo(), null);
	}

}