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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Function;

import com.bigcloud.djomo.annotation.Embed;
import com.bigcloud.djomo.annotation.Ignore;
import com.bigcloud.djomo.annotation.Property;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseObjectModel;

public abstract class ObjectMethodsModel<T> extends BaseObjectModel<T> {

	public ObjectMethodsModel(Type type, ModelContext context) throws IllegalAccessException {
		super(type, context);
	}

	protected void processMethods(MethodHandles.Lookup lookup, ModelContext context,
			Function<String, BeanField.Builder> fieldLookup) throws IllegalAccessException {
		Method[] methods = type.getMethods();
		for (Method method : methods) {
			String name = method.getName();
			if (!Modifier.isStatic(method.getModifiers())) {
				// non static methods, check for getters and setters
				if (name.startsWith("get") && name.length() > 3 && method.getParameterCount() == 0
						&& method.trySetAccessible()) {
					name = name.substring(3, 4).toLowerCase().concat(name.substring(4));
					if (!"class".equals(name)) {
						accessor(lookup, context, getFieldBuilder(fieldLookup, method, name), method, typeArgs);
					}
				} else if (name.startsWith("is") && name.length() > 2 && method.getParameterCount() == 0
						&& method.trySetAccessible()) {
					name = name.substring(2, 3).toLowerCase().concat(name.substring(3));
					accessor(lookup, context, getFieldBuilder(fieldLookup, method, name), method, typeArgs);
				} else {
					processMethod(method, lookup, context, fieldLookup);
				}
			} else {
				processMethod(method, lookup, context, fieldLookup);
			}
		}
	}

	protected BeanField.Builder getFieldBuilder(Function<String, BeanField.Builder> lookup, AnnotatedElement element, String name) {
		var builder = lookup.apply(name);
		Property p = getAnnotation(Property.class, element, name);
		if (p != null) {
			builder.annotation(p);
		}
		Ignore i = getAnnotation(Ignore.class, element, name);
		if (i != null) {
			builder.annotation(i);
		}
		Embed e = getAnnotation(Embed.class, element, name);
		if(e != null) {
			builder.annotation(e);
		}
		return builder;
	}

	private <T extends Annotation> T getAnnotation(Class<T> annotationClass, AnnotatedElement element, String name) {
		T anno = element.getAnnotation(annotationClass);
		if (anno != null) {
			return anno;
		}
		Class checkClass = type;
		while(checkClass != null) {
			try {
				var f = checkClass.getDeclaredField(name);
				return f.getAnnotation(annotationClass);
			} catch (NoSuchFieldException | SecurityException e) {
				checkClass = checkClass.getSuperclass();
			}
		}
		return null;
	}

	protected abstract void processMethod(Method method, MethodHandles.Lookup lookup, ModelContext context,
			Function<String, BeanField.Builder> fieldLookup) throws IllegalAccessException;
}
