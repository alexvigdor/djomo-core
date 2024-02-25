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
package com.bigcloud.djomo.rs.test;

import java.util.stream.Stream;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.visitors.ObjectVisitor;

public class ThingFlattener implements ObjectVisitor {

	@Override
	public void visitObject(Object object, ObjectModel model, Visitor visitor) {
		if(object instanceof Thing in) {
			visitor.visit( Stream.concat(Stream.of(in.name()), in.elements().stream()));
		}
		else {
			visitor.visitObject(object, model);
		}
	}
}
