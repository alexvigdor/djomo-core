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
package com.bigcloud.djomo.poly;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.annotation.Resolve;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ModelFactory;

public class ResolverModelFactory implements ModelFactory {
	private final ConcurrentHashMap<Type, Resolver<?>> resolvers = new ConcurrentHashMap<>();

	public ResolverModelFactory(Resolver<?>... resolvers) {
		for (var r : resolvers) {
			this.resolvers.put(r.getType(), r);
		}
	}

	@Override
	public Model<?> create(Type type, ModelContext context) {
		Class<?> rawType;
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			rawType = (Class<?>) pt.getRawType();
		} else {
			rawType = (Class<?>) type;
		}
		Resolver<?> r = resolvers.get(rawType);
		if (r == null) {
			Resolve resolve = rawType.getAnnotation(Resolve.class);
			if (resolve != null) {
				try {
					r = resolve.value().getConstructor().newInstance();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					throw new RuntimeException("Unable to instantiate Resolver " + resolve.value().getName(), e);
				}
			}
		}
		if (r == null) {
			return null;
		}
		return new ResolverModel<>(type, context, r);
	}

}
