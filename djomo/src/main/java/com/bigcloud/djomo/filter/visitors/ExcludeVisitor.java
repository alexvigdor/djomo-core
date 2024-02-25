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

import java.util.Set;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
/**
 * Prevents fields from being visited
 * 
 * @author Alex Vigdor
 *
 */
public class ExcludeVisitor extends BaseVisitorFilter {
	final Class<?> type;
	final FilterFieldObjectModels excludeModels;

	public ExcludeVisitor(Class<?> type, String... fields) {
		Set<String> excludes = Set.of(fields);
		excludeModels = new FilterFieldObjectModels(model -> model.fields().stream().filter(f -> !excludes.contains(f.key().toString())));
		this.type = type;
	}

	@Override
	public <O> void visitObject(O obj, ObjectModel<O> model) {
		if (type.isInstance(obj)) {
			visitor.visitObject(obj, excludeModels.getFilteredObjectModel(model));
		} else {
			visitor.visitObject(obj, model);
		}
	}
}
