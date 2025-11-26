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
package com.bigcloud.djomo.list;

import java.util.List;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseModelFactory;
import com.bigcloud.djomo.simple.ByteArrayModel;
import com.bigcloud.djomo.simple.CharArrayModel;

public class ListModelFactory extends BaseModelFactory {
	MethodHandles.Lookup lookup = MethodHandles.lookup();

	@Override
	public Model<?> create(Type type, ModelContext context) {
		Class<?> rawType = getRawType(type);
		Type valueType = getTypeParameter(type, 0);
		Constructor<?> constructor = getConstructor(rawType);
		if(rawType.isInterface() && (rawType==List.class || rawType==Collection.class || rawType == Iterable.class)){
			if(valueType == String.class) {
				return new StringListModel(type, context);
			}
			return new ImmutableListModel(type, context, valueType);
		}
		if (Collection.class.isAssignableFrom(rawType)) {
			try {
				MethodHandle handle = constructor == null ? null : lookup.unreflectConstructor(constructor);
				if(valueType == String.class) {
					return new StringCollectionModel<>(type, context, handle);
				}
				return new CollectionModel<>(type, context, handle, valueType);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else if (rawType.isArray()) {
			if (rawType.getComponentType() == char.class) {
				return new CharArrayModel(context);
			}
			if (rawType.getComponentType() == byte.class) {
				return new ByteArrayModel(context);
			}
			if(rawType.getComponentType() == long.class) {
				return new LongArrayModel(context);
			}
			if(rawType.getComponentType() == int.class) {
				return new IntArrayModel(context);
			}
			if(rawType.getComponentType() == double.class) {
				return new DoubleArrayModel(context);
			}
			if(rawType.getComponentType() == String.class) {
				return new StringArrayModel(context);
			}
			return new ArrayModel<>(type, context);
		} else if(Stream.class.isAssignableFrom(rawType)) {
			return new StreamModel<>(type, context, valueType);
		}
		return null;
	}

}
