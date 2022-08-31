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

import java.io.EOFException;
import java.io.IOException;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.error.UnexpectedPrimitiveException;

public class ErrorTest {
	Json Json = new Json();
	@Test
	public void ok1() throws IOException {
		Json.fromString("[null]");
	}
	@Test(expectedExceptions = {UnexpectedPrimitiveException.class})
	public void typo1() throws IOException {
		Json.fromString("[nul]");
	}
	@Test
	public void ok2() throws IOException {
		Json.fromString("[false]");
	}
	@Test(expectedExceptions = {UnexpectedPrimitiveException.class})
	public void typo2() throws IOException {
		Json.fromString("[fals]");
	}
	@Test
	public void ok3() throws IOException {
		Json.fromString("[true]");
	}
	@Test(expectedExceptions = {UnexpectedPrimitiveException.class})
	public void typo3() throws IOException {
		Json.fromString("[tru]");
	}
	@Test(expectedExceptions = {ModelException.class})
	public void typo4() throws IOException {
		Json.fromString("{\"ok\":\"");
	}
	@Test(expectedExceptions = {ModelException.class})
	public void typo5() throws IOException {
		Json.fromString("{\"ok\":");
	}
	@Test(expectedExceptions = {ModelException.class})
	public void typo6() throws IOException {
		Json.fromString("{\"ok\":\"\\");
	}
	@Test(expectedExceptions = {UnexpectedPrimitiveException.class})
	public void typo7() throws IOException {
		Json.fromString("{\"ok\":fal");
	}
	@Test(expectedExceptions = {ModelException.class})
	public void typo8() throws IOException {
		Json.fromString("{\"ok\":\"\\u");
	}
}
