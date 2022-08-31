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
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.filter.FilterVisitor;

/**
 * Baseline visitor behavior, with filtering, dispatch and recursion
 * 
 * @author Alex Vigdor
 *
 */
public abstract class BaseVisitor implements Visitor {

	protected final Models models;
	protected final Visitor current;
	protected final SimpleModel<String> stringModel;

	public BaseVisitor(Models models, FilterVisitor... filters) {
		this.models = models;
		this.stringModel = models.stringModel;
		Visitor curr = this;
		if (filters != null) {
			for (int i = filters.length - 1; i >= 0; i--) {
				curr = filters[i].visitor(curr);
			}
		}
		this.current = curr;
	}

	@Override
	public void visitNull() {
	}

	@Override
	public <T> void visitSimple(T model, SimpleModel<T> definition) {
	}

	@Override
	public <T> void visitList(T model, ListModel<T, ?, ?> definition) {
		definition.forEachItem(model, current::visitListItem);
	}

	@Override
	public <T> void visitObject(T model, ObjectModel<T, ?, ?, ?, ?> definition) {
		definition.forEachField(model, current::visitObjectField);
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		current.visit(value);
	}

	@Override
	public void visitListItem(Object obj) {
		current.visit(obj);
	}

	@Override
	public void visit(Object o) {
		if (o instanceof String) {
			current.visitSimple((String) o, stringModel);
		} else if (o instanceof Integer) {
			current.visitSimple((Integer) o, models.intModel);
		} else if (o instanceof Double) {
			current.visitSimple((Double) o, models.doubleModel);
		} else if (o instanceof Boolean) {
			current.visitSimple((Boolean) o, models.booleanModel);
		} else if (o instanceof Long) {
			current.visitSimple((Long) o, models.longModel);
		} else if (o instanceof Float) {
			current.visitSimple((Float) o, models.floatModel);
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