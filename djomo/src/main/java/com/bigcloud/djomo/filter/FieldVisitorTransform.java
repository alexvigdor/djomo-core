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
package com.bigcloud.djomo.filter;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Transform the value of a Model Field when the field name and type match.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of object that owns the field to be filtered
 * @param <F> The data type of the source field in the model
 */
public abstract class FieldVisitorTransform<T, F> extends FilterVisitor {
	final String field;
	final Class<T> type;
	final Class<F> fieldType;
	boolean typeMatch;

	public abstract Object transform(F in);

	public FieldVisitorTransform(String field) {
		type = ConcreteType.get(this.getClass(), 0);
		fieldType = ConcreteType.get(this.getClass(), 1);
		this.field = field;
	}

	public FieldVisitorTransform(Class<T> type, Class<F> fieldType, String field) {
		this.type = type;
		this.fieldType = fieldType;
		this.field = field;
	}

	public Class<T> getType() {
		return type;
	}

	public Class<F> getFieldType() {
		return fieldType;
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		if (value != null && typeMatch && fieldType.isInstance(value) && field.equals(name)) {
			value = transform((F) value);
		}
		visitor.visitObjectField(name, value);
	}

	@Override
	public <O> void visitObject(O model, ObjectModel<O, ?, ?, ?, ?> definition) {
		var otm = typeMatch;
		typeMatch = type.isInstance(model);
		visitor.visitObject(model, definition);
		typeMatch = otm;
	}

}
