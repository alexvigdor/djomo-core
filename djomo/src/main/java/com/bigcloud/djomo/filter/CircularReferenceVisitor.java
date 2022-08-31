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

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Keeps a local stack of objects being visited, and performs an identity equals check to prevent circular references from being visited.
 * 
 * @author Alex Vigdor
 *
 */
public class CircularReferenceVisitor extends FilterVisitor {

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

	@Override
	public void visitListItem(Object obj) {
		if (!context.contains(obj)) {
			visitor.visitListItem(obj);
		}
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		if (!context.contains(value)) {
			visitor.visitObjectField(name, value);
		}
	}

	@Override
	public <T> void visitObject(T o, ObjectModel<T, ?, ?, ?, ?> d) {
		Context c = context;
		context = new Context(o, c);
		visitor.visitObject(o, d);
		context = c;
	}

	@Override
	public <T> void visitList(T o, ListModel<T, ?, ?> d) {
		Context c = context;
		context = new Context(o, c);
		visitor.visitList(o, d);
		context = c;
	}
	public CircularReferenceVisitor clone() {
		CircularReferenceVisitor f = (CircularReferenceVisitor) super.clone();
		f.context = new Context(null, null);
		return f;
	}
}
