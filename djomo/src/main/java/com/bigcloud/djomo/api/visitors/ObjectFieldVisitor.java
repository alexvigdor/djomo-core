/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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
package com.bigcloud.djomo.api.visitors;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.api.VisitorFilterFactory;
import com.bigcloud.djomo.api.VisitorTypedFilterFactory;
import com.bigcloud.djomo.base.BaseVisitorFilter;

@FunctionalInterface
public interface ObjectFieldVisitor extends VisitorFilterFactory, VisitorTypedFilterFactory {
	void visitObjectField(Object name, Visitor visitor);

	@Override
	default VisitorFilter newVisitorFilter() {
		return new BaseVisitorFilter() {
			@Override
			public void visitObjectField(Object name) {
				ObjectFieldVisitor.this.visitObjectField(name, visitor);
			}

		};
	}

	@Override
	default VisitorFilter newVisitorFilter(Class boundingType) {
		return new BaseVisitorFilter() {
			boolean bound = false;

			@Override
			public void visitObjectField(Object name) {
				if (bound) {
					ObjectFieldVisitor.this.visitObjectField(name, visitor);
				}
				visitor.visitObjectField(name);
			}

			@Override
			public void visitObject(Object obj, ObjectModel model) {
				var wb = bound;
				bound = boundingType.isInstance(obj);
				visitor.visitObject(obj, model);
				bound = wb;
			}

		};
	}
}
