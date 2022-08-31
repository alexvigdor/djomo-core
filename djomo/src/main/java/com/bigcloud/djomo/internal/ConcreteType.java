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
package com.bigcloud.djomo.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ConcreteType {
	public static <T> Class<T> get(Class<?> source, int typeIndex) {
		Type genericSuper = source.getGenericSuperclass();
		if(!(genericSuper instanceof ParameterizedType)) {
			throw new IllegalArgumentException("ConcreteType can only be looked up from a ParameterizedType, not "+genericSuper.getTypeName());
		}
		var concreteType = ((ParameterizedType) genericSuper).getActualTypeArguments()[typeIndex];
		if (concreteType instanceof Class) {
			return (Class<T>) concreteType;
		}
		if (concreteType instanceof ParameterizedType) {
			return (Class<T>) ((ParameterizedType) concreteType).getRawType();
		}
		String simpleName = source.getSimpleName();
		if(simpleName.isBlank()) {
			simpleName = ((Class) ((ParameterizedType) genericSuper).getRawType()).getSimpleName();
		}
		StringBuilder exb = new StringBuilder(simpleName);
		exb.append("<");
		boolean first = true;
		for(var ta : ((ParameterizedType) genericSuper).getActualTypeArguments()) {
			if(first) {
				first = false;
			}
			else {
				exb.append(", ");
			}
			if(ta instanceof Class) {
				exb.append(((Class)ta).getSimpleName());
			}
			else {
				exb.append("Object");
			}
		}
		exb.append(">");
		exb.append("() {}");
		throw new IllegalArgumentException("No concrete type found for " + concreteType.getTypeName() + " on "
				+ genericSuper.getTypeName()
				+ "; you must define a subclass with concrete type arguments, e.g. `new " + exb + "`");
	}
}
