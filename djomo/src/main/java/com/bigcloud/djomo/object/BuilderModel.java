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

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ModelContext;

public class BuilderModel<T> extends ObjectMethodsModel<T> {
	final MethodHandle builderMethod;
	final MethodHandle buildMethod;
	Class builderClass = null;

	public BuilderModel(Type type, ModelContext context, MethodHandle builderMethod, MethodHandle buildMethod) throws IllegalAccessException {
		super(type, context);
		this.builderMethod = builderMethod;
		this.buildMethod = buildMethod;
	}

	@Override
	public Object maker() {
		try {
			return builderMethod.invoke();
		} catch ( Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected Map<CharSequence, Field> initFields(ModelContext context) throws IllegalAccessException {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		var fields = new ConcurrentHashMap<String, BeanField.Builder>();
		Function<String, BeanField.Builder> fieldLookup = (name) -> fields.computeIfAbsent(name,
				n -> BeanField.builder().name(n));
		processMethods(lookup, context, fieldLookup);
		for (Method method : builderClass.getMethods()) {
			if (!Modifier.isStatic(method.getModifiers())) {
				String name = method.getName();
				if (method.getParameterCount() == 1 && method.trySetAccessible()) {
					if("wait".equals(name) || "equals".equals(name)) {
						continue;
					}
					// setter
					if (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
						name = name.substring(3, 4).toLowerCase().concat(name.substring(4));
					}
					mutator(lookup, context, fieldLookup.apply(name), method, typeArgs);
				} 
			}
		}
		return fields.entrySet().stream()
				.map(e -> new AbstractMap.SimpleEntry<String, Field>(e.getKey(), e.getValue().build()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	protected void processMethod(Method method, MethodHandles.Lookup lookup, ModelContext context,
			Function<String, BeanField.Builder> fieldLookup) {
		String name = method.getName();
		if (Modifier.isStatic(method.getModifiers()) && (name.contains("builder") || name.contains("Builder"))
				&& method.getParameterCount() == 0 && method.trySetAccessible()) {
			// fields can't be static methods, but builders can
			Class bc = method.getReturnType();
			try {
				Method bm = bc.getDeclaredMethod("build");
				if (bm != null && bm.getReturnType().equals(type) && bm.trySetAccessible()) {
					builderClass = bc;
				}
			} catch (NoSuchMethodException | SecurityException e1) {
				return;
			}
		}
	}

	@Override
	public T make(Object maker) {
		try {
			return (T) buildMethod.invoke(maker);
		} catch ( Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
