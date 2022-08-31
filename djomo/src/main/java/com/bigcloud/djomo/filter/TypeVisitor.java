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

import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Selectively apply another FilterVisitor only when visiting a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the wrapped filter should be applied
 */
public class TypeVisitor<T> extends FilterVisitor {
	final Class<T> type;
	FilterVisitor filterVisitor;
	Visitor target;

	public TypeVisitor(FilterVisitor filterVisitor) {
		type = ConcreteType.get(this.getClass(), 0);
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitor(int index, FilterVisitor filterVisitor) {
		type = ConcreteType.get(this.getClass(), index);
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitor(Class<T> type, FilterVisitor filterVisitor) {
		this.type = type;
		this.filterVisitor = filterVisitor;
	}

	public Class<T> getType() {
		return type;
	}

	public TypeVisitor<T> clone() {
		TypeVisitor<T> clone = (TypeVisitor<T>) super.clone();
		clone.filterVisitor = filterVisitor.clone();
		return clone;
	}

	@Override
	public TypeVisitor<T> visitor(Visitor visitor) {
		this.target = visitor;
		var fv = filterVisitor;
		if(this.visitor != fv) {
			this.visitor = visitor;
		}
		fv.visitor(visitor);
		return this;
	}

	@Override
	public void visit(Object obj) {
		var c = this.visitor;
		if (obj != null && type.isInstance(obj)) {
			(this.visitor = filterVisitor).visit(obj);
		} else {
			(this.visitor = target).visit(obj);
		}
		this.visitor = c;
	}

}
