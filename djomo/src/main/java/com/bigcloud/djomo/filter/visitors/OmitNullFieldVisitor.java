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

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;

/**
 * Prevent null field values from being visited
 * 
 * @author Alex Vigdor
 *
 */
public class OmitNullFieldVisitor extends BaseVisitorFilter {
	FilterFieldObjectModels omitModels;

	public OmitNullFieldVisitor() {
		omitModels = new FilterFieldObjectModels(stream -> stream.map(this::filterField));
	}
	
	private Field filterField(Field field) {
		if (field.model().getType().isPrimitive()) {
			return field;
		}
		return new FilterField(field) {
			@Override
			public void visit(Object source, Visitor visitor) {
				Object val = get(source);
				if (val != null) {
					visitor.visitObjectField(key());
					visitor.visit(val, model());
				}
			}
		};
	}
	
	@Override
	public <O> void visitObject(O obj, ObjectModel<O> model) {
		visitor.visitObject(obj, omitModels.getFilteredObjectModel(model));
	}

}
