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
import java.io.StringReader;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;

import static org.testng.Assert.*;

import lombok.Builder;
import lombok.Value;

public class FieldNamesTest {

	@Test
	public void testNameLookup() throws IOException {
		Json Json = new Json();
		String sampleData = Json.toString(Sample.builder()
				.field1("value1")
				.field2("value2")
				.field3("value3")
				.field4("value4")
				.field5("value5")
				.field6("value6")
				.field7("value7")
				.field8("value8")
				.field9("value9")
				.build());
		Sample data = Json.read(new StringReader(sampleData), Sample.class);
		assertEquals(data.field1, "value1");
		assertEquals(data.field2, "value2");
		assertEquals(data.field3, "value3");
		assertEquals(data.field4, "value4");
		assertEquals(data.field5, "value5");
		assertEquals(data.field6, "value6");
		assertEquals(data.field7, "value7");
		assertEquals(data.field8, "value8");
		assertEquals(data.field9, "value9");
		
	}
	
	@Builder
	@Value
	static public class Sample{
		String field1;
		String field2;
		String field3;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;

		String field4;
	}
}
