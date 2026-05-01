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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
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
		if (Collection.class.isAssignableFrom(rawType)) {
			Supplier<Collection> supplier;
			if (rawType == List.class || rawType == Collection.class || rawType == ArrayList.class || rawType == Iterable.class) {
				supplier = () -> new ArrayList(122);
			} else if (constructor != null) {
				try {
					final MethodHandle handle = lookup.unreflectConstructor(constructor);
					supplier = () -> {
						try {
							return (Collection) handle.invoke();
						} catch (Throwable e) {
							throw new RuntimeException("Failed to create collection of type " + rawType, e);
						}
					};
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			} else {
				supplier = () -> {
					throw new UnsupportedOperationException("No default constructor for " + rawType);
				};
			}
			if (valueType == String.class) {
				return new StringCollectionModel<>(type, context, supplier);
			}
			return new CollectionModel<>(type, context, supplier, valueType);
		} else if (rawType.isArray()) {
			if (rawType.getComponentType() == char.class) {
				return new CharArrayModel(context);
			}
			if (rawType.getComponentType() == byte.class) {
				return new ByteArrayModel(context);
			}
			if (rawType.getComponentType() == long.class) {
				return new LongArrayModel(context);
			}
			if (rawType.getComponentType() == int.class) {
				return new IntArrayModel(context);
			}
			if (rawType.getComponentType() == double.class) {
				return new DoubleArrayModel(context);
			}
			if (rawType.getComponentType() == String.class) {
				return new StringArrayModel(context);
			}
			return new ArrayModel<>(type, context);
		} else if (Stream.class.isAssignableFrom(rawType)) {
			return new StreamModel<>(type, context, valueType);
		}
		return null;
	}

}
