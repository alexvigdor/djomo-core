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
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.filter.visitors.MultiFilterVisitor;

/**
 * <p>
 * Base class for FilterVisitors that can be applied to Json write operations.
 * </p>
 * <p>
 * Default implementation passes through all methods to the underlying visitor;
 * subclasses can override just the visit* methods of interest. Subclass methods
 * should generally invoke the corresponding super method on this base class, or
 * on the wrapped visitor directly, unless the intention is to modify the data
 * structure.
 * </p>
 * <p>
 * Subclasses MUST provide a proper clone() implementation that invokes
 * super.clone(); FilterVisitors loaded from annotations and/or applied to a
 * JsonBuilder are cloned before each invocation, so it is safe to have
 * processing state stored in filter instance fields.
 * </p>
 * 
 * @author Alex Vigdor
 *
 */
public class BaseVisitorFilter implements VisitorFilter, Cloneable {
	protected Visitor visitor;

	public BaseVisitorFilter() {
	}

	public BaseVisitorFilter(Visitor visitor) {
		this.visitor = visitor;
	}

	public void filter(Visitor visitor) {
		this.visitor = visitor;
	}

	@Override
	public void visitNull() {
		visitor.visitNull();
	}

	@Override
	public <T> void visitList(T model, ListModel<T> definition) {
		visitor.visitList(model, definition);
	}

	@Override
	public void visitListItem() {
		visitor.visitListItem();
	}

	@Override
	public <T> void visitObject(T model, ObjectModel<T> definition) {
		visitor.visitObject(model, definition);
	}

	@Override
	public void visitObjectField(Object name) {
		visitor.visitObjectField(name);
	}

	@Override
	public void visit(Object obj) {
		visitor.visit(obj);
	}

	@Override
	public Models models() {
		return visitor.models();
	}

	public BaseVisitorFilter clone() {
		try {
			BaseVisitorFilter clone = (BaseVisitorFilter) super.clone();
			clone.visitor = null;
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visitInt(int value) {
		visitor.visitInt(value);
	}

	@Override
	public void visitLong(long value) {
		visitor.visitLong(value);
	}

	@Override
	public void visitFloat(float value) {
		visitor.visitFloat(value);
	}

	@Override
	public void visitDouble(double value) {
		visitor.visitDouble(value);
	}

	@Override
	public void visitBoolean(boolean value) {
		visitor.visitBoolean(value);
	}

	@Override
	public void visitString(CharSequence value) {
		visitor.visitString(value);
	}

	public BaseVisitorFilter and(BaseVisitorFilter... next) {
		return new MultiFilterVisitor(this, next);
	}

	@Override
	public BaseVisitorFilter newVisitorFilter() {
		return clone();
	}
}
