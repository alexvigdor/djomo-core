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
package com.bigcloud.djomo.filter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;

/**
 * Manage a cache of filtered field models; this is useful for dealing with
 * filter logic that might apply to a base class or interface, applying it to
 * each concrete sub-type independently.
 * 
 * @author Alex Vigdor
 *
 */
public class FilterFieldObjectModels {
	@SuppressWarnings("rawtypes")
	final ConcurrentHashMap<ObjectModel, ObjectModel> models = new ConcurrentHashMap<>();
	final Function<Stream<Field>, Stream<Field>> fieldsFilter;

	public FilterFieldObjectModels(Function<Stream<Field>, Stream<Field>> fieldsFilter) {
		this.fieldsFilter = fieldsFilter;
	}

	@SuppressWarnings("unchecked")
	public ObjectModel getFilteredObjectModel(ObjectModel model) {
		return models.computeIfAbsent(model, om -> {
			List<Field> fields = om.fields();
			if (fields != null) {
				return new FilterFieldObjectModel(om, fieldsFilter.apply(fields.stream()));
			} else {
				return new FilterObjectModel(om) {
					@Override
					public void visitFields(Object t, Visitor visitor) {
						Stream<Field> fields = fieldsFilter.apply(om.fields(t));
						fields.forEach(field -> field.visit(t, visitor));
					}

					@Override
					public Field getField(CharSequence name) {
						return fieldsFilter.apply(Stream.of(model.getField(name))).findFirst().orElse(null);
					}
				};
			}
		});
	}

}
