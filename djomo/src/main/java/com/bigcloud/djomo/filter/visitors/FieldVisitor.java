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

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.api.VisitorFilterFactory;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
import com.bigcloud.djomo.internal.ConcreteType;

/**
 * Selectively apply another FilterVisitor only when within the specified field
 * of the declared type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of object to filter by field
 */
public class FieldVisitor<T> extends BaseVisitorFilter {
	final FilterFieldObjectModels fieldModels;
	final Class<T> type;

	public FieldVisitor(String field, VisitorFilterFactory filterVisitor) {
		type = ConcreteType.get(this.getClass(), 0);
		this.fieldModels = init(field, filterVisitor);
	}

	public FieldVisitor(Class<T> type, String field, VisitorFilterFactory filterVisitor) {
		this.type = type;
		this.fieldModels = init(field, filterVisitor);
	}

	private FilterFieldObjectModels init(String field, VisitorFilterFactory filterVisitor) {
		return new FilterFieldObjectModels(model -> model.fields().stream().map(f -> {
			if (f.key().toString().equals(field)) {
				return new FilterField(f) {
					@Override
					public void visit(Object source, Visitor visitor) {
						VisitorFilter isolated = filterVisitor.newVisitorFilter();
						isolated.filter(visitor);
						super.visit(source, isolated);
					}
				};
			}
			return f;
		}));
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public <O> void visitObject(O model, ObjectModel<O> definition) {
		if (type.isInstance(model)) {
			visitor.visitObject(model, fieldModels.getFilteredObjectModel(definition));
		} else {
			visitor.visitObject(model, definition);
		}
	}

}
