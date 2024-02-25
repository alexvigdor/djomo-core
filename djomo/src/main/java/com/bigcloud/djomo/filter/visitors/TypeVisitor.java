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
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Selectively apply another FilterVisitor only when visiting a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the wrapped filter should be applied
 */
public class TypeVisitor<T> extends BaseVisitorFilter {
	final Class<T> type;
	VisitorFilter filterVisitor;
	Visitor target;

	public TypeVisitor(VisitorFilter filterVisitor) {
		type = ConcreteType.get(this.getClass(), 0);
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitor(int index, VisitorFilter filterVisitor) {
		type = ConcreteType.get(this.getClass(), index);
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitor(Class<T> type, VisitorFilter filterVisitor) {
		this.type = type;
		this.filterVisitor = filterVisitor;
	}

	public Class<T> getType() {
		return type;
	}

	public TypeVisitor<T> clone() {
		TypeVisitor<T> clone = (TypeVisitor<T>) super.clone();
		clone.filterVisitor = filterVisitor.newVisitorFilter();
		return clone;
	}

	@Override
	public void filter(Visitor visitor) {
		this.target = visitor;
		var fv = filterVisitor;
		if(this.visitor != fv) {
			this.visitor = visitor;
		}
		fv.filter(visitor);
	}

	@Override
	public <T> void visitObject(T obj, ObjectModel<T> model) {
		var c = this.visitor;
		if (obj != null && type.isInstance(obj)) {
			(this.visitor = filterVisitor).visitObject(obj, model);
		} else {
			(this.visitor = target).visitObject(obj, model);
		}
		this.visitor = c;
	}


}
