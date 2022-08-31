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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Define a subset of fields from the given model that will be pulled through the parser.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to filter by only parsing the defined fields
 */
public class IncludeParser<T> extends FilterParser {
	final Class<T> type;
	final ObjectModel includeModel;

	public IncludeParser(Model<T> model, String... fields) {
		if (!(model instanceof ObjectModel)) {
			throw new IllegalArgumentException();
		}
		ObjectModel<T, ?, ?, ?, ?> om = (ObjectModel<T, ?, ?, ?, ?>) model;
		type = model.getType();
		ArrayDeque<Field<T, ?, ?>> resolved = new ArrayDeque<>();
		Map<String, Field> fieldsByName = new HashMap<>();
		for (String fn : fields) {
			Field<T, ?, ?> f = om.getField(fn);
			if (f != null) {
				fieldsByName.put(f.key().toString(), f);
				resolved.add(f);
			}
		}
		final List<Field> fieldList = List.of(resolved.toArray(new Field[0]));
		includeModel = new FilterObjectModel(om) {
			@Override
			public Field getField(String name) {
				return fieldsByName.get(name);
			}
			@Override
			public List<Field> fields() {
				return fieldList;
			}
		};
	}

	@Override
	public <O> O parse(Model<O> model) {
		if (type.isAssignableFrom(model.getType())) {
			return (O) parser.parse(includeModel);
		} else {
			return parser.parse(model);
		}
	}
}
