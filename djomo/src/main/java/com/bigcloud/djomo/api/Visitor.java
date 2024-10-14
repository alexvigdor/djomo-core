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
package com.bigcloud.djomo.api;

import com.bigcloud.djomo.Models;

/**
 * Interface for object visitors, such as serializers; customizing serialization
 * is typically done by extending the FilterVisitor base class.
 * 
 * @author Alex Vigdor
 *
 */
public interface Visitor {
	void visitNull();

	<T> void visitList(T list, ListModel<T> model);

	void visitListItem();

	<T> void visitObject(T object, ObjectModel<T> model);

	void visitObjectField(Object name);
	
	<T> void visit(T object, Model<T> model);

	void visit(Object obj);

	void visitInt(int value);

	void visitLong(long value);

	void visitFloat(float value);

	void visitDouble(double value);

	void visitBoolean(boolean value);

	void visitString(CharSequence value);

	Models models();

}
