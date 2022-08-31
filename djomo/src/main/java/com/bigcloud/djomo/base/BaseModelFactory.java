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
package com.bigcloud.djomo.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ModelFactory;

public abstract class BaseModelFactory implements ModelFactory {
	protected Type getTypeParameter(Type type, int index){
		if (!(type instanceof ParameterizedType)) {
			return null;
		}
		ParameterizedType pt = (ParameterizedType) type;
		var ats = pt.getActualTypeArguments();
		if (index < ats.length) {
			return ats[index];
		}
		return null;
	}
	protected Class<?> getRawType(Type type){
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			return (Class<?>) pt.getRawType();
		} else {
			return (Class<?>) type;
		}
	}
	protected <T> Constructor<T> getConstructor(Class<T> type) {
		try {
			Constructor<T> constructor = type.getConstructor();
			if (constructor.trySetAccessible()) {
				return constructor;
			}
			return null;
		} catch (NoSuchMethodException | SecurityException e1) {
			return null;
		}
	}
}
