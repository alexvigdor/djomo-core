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

import lombok.Builder;
import lombok.Value;

/**
 * Tests for object models whose property names contain non-ascii characters.
 * <p>
 * Java identifiers may contain any unicode letter, so a bean with a field
 * named {@code café} is legal, and the field-name lookup used by object models
 * (and by the parser when matching json property names) matches non-ascii
 * characters against their escape expansion.
 * <ul>
 * <li>{@link #testUnicodeFieldRoundTrip} round-trips a bean with a non-ascii
 * field name.</li>
 * <li>{@link #testUnicodePropertyOnAsciiBean} checks that an unknown property
 * with a non-ascii name is skipped like any other unknown property when
 * parsing an <em>all-ascii</em> bean.</li>
 * </ul>
 */
public class UnicodeFieldNameTest {

	@Value
	@Builder
	public static class UnicodeFieldBean {
		String café;
		String name;
	}

	@Value
	@Builder
	public static class NameBean {
		String name;
	}

	@Test
	public void testUnicodeFieldRoundTrip() throws IOException {
		Json json = new Json();
		UnicodeFieldBean bean = UnicodeFieldBean.builder().café("accented").name("plain").build();
		String s = json.toString(bean);
		UnicodeFieldBean rt = json.fromString(s, UnicodeFieldBean.class);
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testUnicodePropertyOnAsciiBean() throws IOException {
		Json json = new Json();
		// an unknown property whose name extends a real field name ("nameé"
		// over "name") must be skipped like any other unknown property: the é
		// matches against its escape expansion and finds no field
		NameBean rt = json.fromString(
				"{\"name\":\"hello\",\"nameé\":\"ignored\"}", NameBean.class);
		Assert.assertEquals(rt.getName(), "hello");
	}
}
