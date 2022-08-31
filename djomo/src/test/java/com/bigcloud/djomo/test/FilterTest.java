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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.filter.CircularReferenceVisitor;

public class FilterTest {
	Json Json = new Json();
	
	List regularList;
	List circularList;
	Map regularObject;
	Map circularObject;
	
	@BeforeClass
	public void setup() {
		regularList = new ArrayList<>();
		circularList = new ArrayList<>();
		regularList.add("a");
		circularList.add("a");
		circularList.add(circularList);
		regularObject = new HashMap<>();
		circularObject = new HashMap<>();
		Map indirectObject = new HashMap();
		indirectObject.put("c", "d");
		regularObject.put("a", "b");
		regularObject.put("e", indirectObject);
		Map indirectCircularObject = new HashMap();
		indirectCircularObject.put("c", "d");
		indirectCircularObject.put("f", circularObject);
		circularObject.put("a", "b");
		circularObject.put("e", indirectCircularObject);
	}

	@Test(expectedExceptions = StackOverflowError.class)
	public void testCircularReferenceFilterObjectFail() {
		Json.toString(regularObject);
		Json.toString(circularObject);
	}
	
	@Test
	public void testCircularReferenceFilterObject() {
		Json.toString(regularObject, new CircularReferenceVisitor());
		Json.toString(circularObject, new CircularReferenceVisitor());
	}
	
	@Test(expectedExceptions = StackOverflowError.class)
	public void testCircularReferenceFilterListFail() {
		Json.toString(regularList);
		Json.toString(circularList);
	}
	
	@Test
	public void testCircularReferenceFilterList() {
		Json.toString(regularList, new CircularReferenceVisitor());
		Json.toString(circularList, new CircularReferenceVisitor());
	}
	
	
}
