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
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseObjectModel;

public class EnumMapModel<T extends EnumMap>
		extends BaseObjectModel<T> {
	final Model keyModel;
	final Model valueModel;

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

	protected Map<CharSequence, Field> initFields(ModelContext context) {
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
		Model valueModel = context.get(valueType);
		try {
			Enum[] values = (Enum[]) enumClass.getDeclaredMethod("values").invoke(null);
			Map<CharSequence, Field> rval = Arrays.stream(values)
					.collect(Collectors.toMap(Enum::name,
							f -> new EnumMapField(f, valueModel)));
			return rval;
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object maker(T obj) {
		var m = newInstance();
		m.putAll(obj);
		return m;
	}

	@Override
	public Object maker() {
		return newInstance();
	}

	@Override
	public void forEachField(T t, BiConsumer consumer) {
		t.forEach(consumer);
	}
	public T newInstance() {
		return (T) new EnumMap(keyModel.getType());
	}

	@Override
	public void visitFields(T t, Visitor visitor) {
		var m = valueModel;
		t.forEach((k, v) -> {
			visitor.visitObjectField(k);
			m.tryVisit(v, visitor);
		});
	}

	@Override
	public T make(Object maker) {
		return (T) maker;
	}
}
