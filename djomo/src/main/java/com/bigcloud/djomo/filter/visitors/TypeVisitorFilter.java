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
/**
 * Selectively apply another FilterVisitor only when visiting a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the wrapped filter should be applied
 */
public class TypeVisitorFilter<T> extends TypeVisitor<T> {
	VisitorFilter filterVisitor;
	Visitor target;

	public TypeVisitorFilter(VisitorFilter filterVisitor) {
		super();
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitorFilter(int index, VisitorFilter filterVisitor) {
		super(index);
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitorFilter(Class<T> type, VisitorFilter filterVisitor) {
		super(type);
		this.filterVisitor = filterVisitor;
	}

	public TypeVisitorFilter<T> clone() {
		TypeVisitorFilter<T> clone = (TypeVisitorFilter<T>) super.clone();
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
	protected void visitType(T obj, ObjectModel<T> model) {
		var fp = filterVisitor;
		var filtered = visitor == fp;
		if(!filtered) {
			visitor = fp;
		}
		fp.visitObject(obj, model);
		if(!filtered) {
			visitor = target;
		}
	}


}
