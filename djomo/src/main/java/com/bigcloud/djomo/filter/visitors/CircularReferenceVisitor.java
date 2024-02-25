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

import java.util.List;
import java.util.Map;

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;

/**
 * Keeps a local stack of objects being visited, and performs an identity equals check to prevent circular references from being visited.
 * 
 * @author Alex Vigdor
 *
 */
public class CircularReferenceVisitor extends BaseVisitorFilter {

	public CircularReferenceVisitor() {
	}

	public static class Context {
		private final Object value;
		private final Context parent;

		private Context(Object value, Context parent) {
			this.parent = parent;
			this.value = value;
		}

		boolean contains(Object o) {
			if (o == value) {
				return true;
			}
			Context p = parent;
			while (p != null) {
				if (o == p.value) {
					return true;
				}
				p = p.parent;
			}
			return false;
		}
	}

	Context context = new Context(null, null);

	public CircularReferenceVisitor clone() {
		CircularReferenceVisitor f = (CircularReferenceVisitor) super.clone();
		f.context = new Context(null, null);
		return f;
	}

	@Override
	public <T> void visitList(T model, ListModel<T> definition) {
		if(context.contains(model)) {
			super.visitList(List.of(), definition.models().listModel);
		}
		else {
			Context c = context;
			context = new Context(model, c);
			super.visitList(model, definition);
			context = c;
		}
		
	}

	@Override
	public <T> void visitObject(T model, ObjectModel<T> definition) {
		if(context.contains(model)) {
			super.visitObject(Map.of(), definition.models().mapModel);
		}
		else {
			Context c = context;
			context = new Context(model, c);
			super.visitObject(model, definition);
			context = c;
		}
	}

}
