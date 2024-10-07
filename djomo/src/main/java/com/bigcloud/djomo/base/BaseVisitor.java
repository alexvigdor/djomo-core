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
package com.bigcloud.djomo.base;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.api.VisitorFilterFactory;

/**
 * Baseline visitor behavior, with filtering, dispatch and recursion
 * 
 * @author Alex Vigdor
 *
 */
public abstract class BaseVisitor implements Visitor {

	protected final Models models;
	protected final Visitor current;

	public BaseVisitor(Models models, VisitorFilterFactory... filters) {
		this.models = models;
		Visitor curr = this;
		if (filters != null) {
			for (int i = filters.length - 1; i >= 0; i--) {
				VisitorFilter filter = filters[i].newVisitorFilter();
				filter.filter(curr);
				curr = filter;
			}
		}
		this.current = curr;
	}

	@Override
	public <T> void visitList(T model, ListModel<T> definition) {
		definition.visitItems(model, current);
	}

	@Override
	public <T> void visitObject(T model, ObjectModel<T> definition) {
		definition.visitFields(model, current);
	}

	@Override
	public void visit(Object o) {
		if (o instanceof String s) {
			current.visitString(s);
		} else if (o instanceof Integer i) {
			current.visitInt(i.intValue());
		} else if (o instanceof Double d) {
			current.visitDouble(d.doubleValue());
		} else if (o instanceof Boolean b) {
			current.visitBoolean(b.booleanValue());
		} else if (o instanceof Long l) {
			current.visitLong(l.longValue());
		} else if (o instanceof Float f) {
			current.visitFloat(f.floatValue());
		} else if (o == null) {
			current.visitNull();
		} else {
			((Model) models.get(o.getClass())).visit(o, current);
		}
	}

	/**
	 * Entry point to allow filters to intercept top-level object visit; subclasses
	 * should extend visit as opposed to filter
	 * 
	 * @param o
	 */
	public final void filter(Object o) {
		current.visit(o);
	}

	@Override
	public Models models() {
		return models;
	}
}