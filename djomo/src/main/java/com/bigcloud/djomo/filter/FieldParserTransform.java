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

import java.util.function.BiConsumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Transform between a JSON source type and target Model Field type.  
 * Useful when default data mapping does not work.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of object that owns the field to be filtered
 * @param <S> The data type found in the Json source
 * @param <F> The data type of the target field in the model
 */
public abstract class FieldParserTransform<T, S, F> extends FilterParser {
	final String field;
	final Class<T> type;
	final Class<S> sourceType;
	final Class<F> fieldType;
	boolean typeMatch;

	public abstract F transform(S in);

	public FieldParserTransform(String field) {
		type = ConcreteType.get(this.getClass(), 0);
		sourceType = ConcreteType.get(this.getClass(), 1);
		fieldType = ConcreteType.get(this.getClass(), 2);
		this.field = field;
	}

	public FieldParserTransform(Class<T> type, Class<S> sourceType, Class<F> fieldType, String field) {
		this.type = type;
		this.sourceType = sourceType;
		this.fieldType = fieldType;
		this.field = field;
	}

	public Class<T> getType() {
		return type;
	}

	public Class<F> getFieldType() {
		return fieldType;
	}

	public Class<S> getSourceType() {
		return sourceType;
	}

	@Override
	public <O, M extends ObjectMaker<O, D, V>, D extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, D, ?, V> model, String field, BiConsumer<D, V> consumer) {
		if (typeMatch && this.field.equals(field)) {
			var passthrough = consumer;
			consumer = (def, val) -> {
				if (val != null && sourceType.isInstance(val) && fieldType.isAssignableFrom(def.model().getType())) {
					val = (V) transform((S) val);
				}
				passthrough.accept(def, val);
			};
		}
		parser.parseObjectField(model, field, consumer);
	}

	@Override
	public <O, M extends ObjectMaker<O, D, V>, D extends Field<O, ?, V>, V> M parseObject(
			ObjectModel<O, M, D, ?, V> model) {
		var otm = typeMatch;
		typeMatch = type.isAssignableFrom(model.getType());
		var result = parser.parseObject(model);
		typeMatch = otm;
		return result;
	}
}
