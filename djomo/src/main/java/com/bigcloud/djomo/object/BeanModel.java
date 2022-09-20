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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bigcloud.djomo.annotation.Ignore;
import com.bigcloud.djomo.api.ModelContext;

public class BeanModel<T> extends ObjectMethodsModel<T, BeanMaker<T>> {
	private final MethodHandle constructor;
	
	public BeanModel(Type type, ModelContext context, MethodHandle constructor) throws IllegalAccessException {
		super(type, context);
		this.constructor = constructor;
	}

	@Override
	public BeanMaker<T> maker() {
		return new BeanMaker<>(this);
	}

	public T newInstance() {
		try {
			return (T) constructor.invoke();
		} catch (Throwable e) {
			throw new RuntimeException("Unable to create instance of "+type.getName(), e);
		}
	}

	@Override
	protected Map<CharSequence, BeanField<T, Object>> initFields(ModelContext context) throws IllegalAccessException {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		var fields = new ConcurrentHashMap<String, BeanField.Builder<T,Object>>();
		Function<String, BeanField.Builder<T, Object>> fieldLookup = (name) -> fields.computeIfAbsent(name,
				n -> BeanField.<T,Object>builder().name(n));
		// start with direct field access; method access will override
		for (var field : type.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Ignore.class) == null && field.trySetAccessible() ) {
				publicField(lookup, context, fieldLookup.apply(field.getName()), field, typeArgs);
			}
		}
		processMethods(lookup, context, fieldLookup);
		return fields.entrySet().stream()
				.map(e -> new AbstractMap.SimpleEntry<String, BeanField<T,Object>>(e.getKey(), e.getValue().build()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	protected void processMethod(Method method, MethodHandles.Lookup lookup, ModelContext context,
			Function<String, BeanField.Builder<T, Object>> fieldLookup) throws IllegalAccessException {
		String name = method.getName();
		if (!Modifier.isStatic(method.getModifiers()) && name.startsWith("set") && name.length() > 3
				&& method.getParameterCount() == 1 && method.trySetAccessible()) {
			name = name.substring(3, 4).toLowerCase().concat(name.substring(4));
			mutator(lookup, context, fieldLookup.apply(name), method, typeArgs);
		}
	}
}
