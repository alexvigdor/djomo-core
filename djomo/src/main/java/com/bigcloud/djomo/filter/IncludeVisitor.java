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
import java.util.List;
import java.util.function.BiConsumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Define an ordered subset of fields from the given model that will be visited.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to filter by only including the given fields
 */
public class IncludeVisitor<T> extends FilterVisitor {
	final Class<T> type;
	final ObjectModel<T, ?, ?, ?, ?> includeModel;

	public IncludeVisitor(Model<T> model, String... fields) {
		if (!(model instanceof ObjectModel)) {
			throw new IllegalArgumentException(model.getClass().getName()+" is not an ObjectModel");
		}
		ObjectModel<T, ?, ?, ?, ?> om = (ObjectModel<T, ?, ?, ?, ?>) model;
		type = model.getType();
		ArrayDeque<Field<T, ?, ?>> resolved = new ArrayDeque<>();
		for (String fn : fields) {
			Field<T, ?, ?> f = om.getField(fn);
			if (f != null) {
				resolved.add(f);
			}
		}
		includeModel = new FilterObjectModel(om) {
			final Field[] useFields = resolved.toArray(new Field[0]);
			final List<Field> fieldList = List.of(useFields);

			@Override
			public void forEachField(Object t, BiConsumer consumer) {
				for (Field f : useFields) {
					consumer.accept(f.key(), f.get(t));
				}
			}
			public List<Field> fields() {
				return fieldList;
			}
		};
	}

	@Override
	public <O> void visitObject(O obj, ObjectModel<O, ?, ?, ?, ?> model) {
		if (type.isInstance(obj)) {
			visitor.visitObject((T) obj, includeModel);
		} else {
			visitor.visitObject(obj, model);
		}
	}
}
