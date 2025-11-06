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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

import com.bigcloud.djomo.annotation.Ignore;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseModelFactory;

public class ObjectModelFactory extends BaseModelFactory {
	MethodHandles.Lookup lookup = MethodHandles.publicLookup();

	@Override
	public Model<?> create(Type type, ModelContext context) {
		try {
			Class<?> rawType = getRawType(type);
			Constructor<?> constructor = getConstructor(rawType);
			if(EnumMap.class.isAssignableFrom(rawType)) {
				return new EnumMapModel<>(type, context);
			}
			else if(Map.class.isAssignableFrom(rawType)) {
				return new MapModel<>(type, context, constructor == null ? null : lookup.unreflectConstructor(constructor));
			}
			else if(rawType.isRecord()) {
			
				return new RecordModel<>(type, context, lookup.unreflectConstructor(rawType.getDeclaredConstructors()[0]));
			}
			else {
				Method[] methods = rawType.getMethods();
				//Object foundBuilder = null;
				Method builderMethod = null;
				Method buildMethod = null;
				for (Method method : methods) {
					if(method.getAnnotation(Ignore.class) != null) {
						continue;
					}
					String name = method.getName();
					// fields can't be static methods, but builders can
					if (Modifier.isStatic(method.getModifiers()) && (name.contains("builder") || name.contains("Builder")) && method.getParameterCount() == 0 && method.trySetAccessible()) {
						Class<?> bc = method.getReturnType();
						try {
							Method bm = bc.getDeclaredMethod("build");
							if(bm!=null && bm.getReturnType().equals(rawType) && bm.trySetAccessible()) {
								builderMethod = method;
							}
						} catch (NoSuchMethodException | SecurityException e1) {
							continue;
						}
					} 
				}
				if (builderMethod != null) {
					for (Method method : builderMethod.getReturnType().getMethods()) {
						if (!Modifier.isStatic(method.getModifiers())) {
							String name = method.getName();
							if (method.getParameterCount() == 0 && name.equals("build") && method.trySetAccessible()) {
								buildMethod = method;
							}
						}
					}
				}
				if(buildMethod!=null && builderMethod != null) {
					return new BuilderModel<>(type, context, lookup.unreflect(builderMethod), lookup.unreflect(buildMethod));
				}
				return new BeanModel<>(type, context, constructor == null ? null : lookup.unreflectConstructor(constructor));
			}
			} catch (IllegalAccessException e1) {
				throw new RuntimeException(e1);
			}
	}

}
