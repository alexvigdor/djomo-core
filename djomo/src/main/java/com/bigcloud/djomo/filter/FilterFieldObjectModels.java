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

/**
 * Manage a cache of filtered field models; this is useful for dealing with
 * filter logic that might apply to a base class or interface, applying it to
 * each concrete sub-type independently.
 * 
 * @author Alex Vigdor
 *
 */
public class FilterFieldObjectModels {
	final ConcurrentHashMap<ObjectModel<?>, ObjectModel<?>> models = new ConcurrentHashMap<>();
	final Function<ObjectModel<?>, Stream<Field>> fieldsFilter;

	public FilterFieldObjectModels(Function<ObjectModel<?>, Stream<Field>> fieldsFilter) {
		this.fieldsFilter = fieldsFilter;
	}

	public ObjectModel getFilteredObjectModel(ObjectModel model) {
		List<Field> fields = model.fields();
		if (fields == null) {
			return model;
		}
		return models.computeIfAbsent(model, om -> new FilterFieldObjectModel(om, fieldsFilter.apply(model)));
	}
}
