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

import java.util.Objects;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
/**
 * Define an ordered subset of fields from the given model that will be visited.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to filter by only including the given fields
 */
public class IncludeVisitor<T> extends BaseVisitorFilter {
	final FilterFieldObjectModels includeModels;
	final Class<T> type;

	public IncludeVisitor(Class<T> type, String... fields) {
		includeModels = new FilterFieldObjectModels(model -> Stream.of(fields).map(model::getField).filter(Objects::nonNull));
		this.type = type;
	}

	@Override
	public <O> void visitObject(O obj, ObjectModel<O> model) {
		if (type.isInstance(obj)) {
			visitor.visitObject((T) obj, includeModels.getFilteredObjectModel(model));
		} else {
			visitor.visitObject(obj, model);
		}
	}
}
