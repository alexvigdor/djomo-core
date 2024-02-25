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

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.api.VisitorFilterFactory;
import com.bigcloud.djomo.base.BaseVisitorFilter;

@FunctionalInterface
public interface ListVisitor extends VisitorFilterFactory {
	void visitList(Object list, ListModel model, Visitor visitor);

	@Override
	default VisitorFilter newVisitorFilter() {
		return new BaseVisitorFilter() {
			@Override
			public <T> void visitList(T list, ListModel<T> model) {
				ListVisitor.this.visitList(list, model, visitor);
			}

		};
	}
}
