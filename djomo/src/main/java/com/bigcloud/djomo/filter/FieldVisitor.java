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
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Selectively apply another FilterVisitor only when within the specified field of the declared type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of object to filter by field
 */
public class FieldVisitor<T> extends FilterVisitor {
	final Class<T> type;
	final String field;
	FilterVisitor filterVisitor;
	Visitor target;
	boolean typeMatch;

	public FieldVisitor(String field, FilterVisitor filterVisitor) {
		type = ConcreteType.get(this.getClass(), 0);
		this.field = field;
		this.filterVisitor = filterVisitor;
	}

	public FieldVisitor(Class<T> type, String field, FilterVisitor filterVisitor) {
		this.type = type;
		this.field = field;
		this.filterVisitor = filterVisitor;
	}

	public Class<T> getType() {
		return type;
	}

	public FieldVisitor<T> clone() {
		FieldVisitor<T> clone = (FieldVisitor<T>) super.clone();
		clone.filterVisitor = filterVisitor.clone();
		return clone;
	}

	@Override
	public FieldVisitor<T> visitor(Visitor visitor) {
		this.target = visitor;
		var fv = filterVisitor;
		if (this.visitor != fv) {
			this.visitor = visitor;
		}
		fv.visitor(visitor);
		return this;
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		Visitor ov = visitor;
		Visitor dest;
		if (typeMatch && this.field.equals(name)) {
			visitor = dest = filterVisitor;
		} else {
			visitor = dest = target;
		}
		dest.visitObjectField(name, value);
		visitor = ov;
	}

	@Override
	public <O> void visitObject(O model, ObjectModel<O, ?, ?, ?, ?> definition) {
		var otm = typeMatch;
		typeMatch = type.isInstance(model);
		visitor.visitObject(model, definition);
		typeMatch = otm;
	}

}
