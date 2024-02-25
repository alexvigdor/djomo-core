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
package com.bigcloud.djomo.filter.visitors;

import java.util.List;
import java.util.function.Consumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterListModel;
/**
 * 
 * Visit an object into a list of field values; designed for symmetric use with 
 * ObjectFieldListParser
 * 
 * @author Alex Vigdor
 *
 */
public class ObjectFieldListVisitor extends BaseVisitorFilter {
	public <T> void visitObject(T obj, ObjectModel<T> model) {
		List<Field> fields = (List<Field>) model.fields();
		if (fields != null) {
			visitList(obj, new FilterListModel<T>(null) {
				public void forEachItem(T obj, Consumer consumer) {
					for (Field f : fields) {
						consumer.accept(f.get(obj));
					}
				}
				@Override
				public void visitItems(T t, Visitor visitor) {
					for (Field f : fields) {
						var v = f.get(obj);
						visitor.visitListItem();
						if(v == null) {
							visitor.visitNull();
						}
						else {
							f.model().visit(v, visitor);
						}
					}
				}
			});
		} else {
			super.visitObject(obj, model);
		}
	}
}