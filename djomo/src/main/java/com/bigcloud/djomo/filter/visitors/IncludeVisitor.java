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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
/**
 * Define a subset of fields from the given model that will be visited.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to filter by only including the given fields
 */
public class IncludeVisitor<T> extends BaseVisitorFilter {
	final FilterFieldObjectModels includeModels;
	final Class<T> type;
	final List<String> includes;

	public IncludeVisitor(Class<T> type, String... fields) {
		includes = List.of(fields);
		includeModels = new FilterFieldObjectModels(this::processIncludes);
		this.type = type;
	}
	
	private Stream<Field> processIncludes(Stream<Field> fields){
		Map<String, Field> fieldsMap = fields.collect(Collectors.toMap(f -> f.key().toString(), Function.identity()));
		return includes.stream().map(fieldsMap::get).filter(Objects::nonNull);
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
