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
package com.bigcloud.djomo.object;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseObjectModel;

public class EnumMapModel<T extends EnumMap<E, V>, E extends Enum<E>, V>
		extends BaseObjectModel<T, EnumMapMaker<T, E, V>, EnumMapField<T, E, V>, E, V> {
	final Model<E> keyModel;
	final Model<V> valueModel;

	public EnumMapModel(Type type, ModelContext context) throws IllegalAccessException {
		super(type, context);
		if (typeArgs != null) {
			Iterator<Type> ti = typeArgs.values().iterator();
			keyModel = context.get(ti.next());
			valueModel = context.get(ti.next());
		} else {
			keyModel = null;
			valueModel = context.get(Object.class);
		}
	}

	protected Map<CharSequence, EnumMapField<T, E, V>> initFields(ModelContext context) {
		if (typeArgs == null) {
			return Collections.emptyMap();
		}
		Iterator<Type> iter = typeArgs.values().iterator();
		Type enumType = iter.next();
		Class<?> enumClass;
		if (enumType instanceof ParameterizedType) {
			enumClass = (Class<?>) ((ParameterizedType) enumType).getRawType();
		} else {
			enumClass = (Class<?>) enumType;
		}
		Type valueType = iter.next();
		Model<V> valueModel = (Model<V>) context.get(valueType);
		try {
			E[] values = (E[]) enumClass.getDeclaredMethod("values").invoke(null);
			Map<CharSequence, EnumMapField<T, E, V>> rval = Arrays.stream(values)
					.collect(Collectors.toMap(E::name,
							f -> new EnumMapField<T, E, V>(f, valueModel)));
			return rval;
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnumMapMaker<T, E, V> maker(T obj) {
		var m = maker();
		m.map.putAll(obj);
		return m;
	}

	@Override
	public EnumMapMaker<T, E, V> maker() {
		return new EnumMapMaker<>(this);
	}

	@Override
	public void forEachField(T t, BiConsumer<E, V> consumer) {
		t.forEach(consumer);
	}
	public T newInstance() {
		return (T) new EnumMap<E, V>(keyModel.getType());
	}

	@Override
	public List<EnumMapField<T, E, V>> fields() {
		return null;
	}
}
