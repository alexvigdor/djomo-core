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
package com.bigcloud.djomo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * Provides an easy way to programmatically define a complex generic type for parsing, e.g.
 * 
 * Json.read(..., ModelType.of(Map.class, Integer.class, ModelType.of(List.class, ZonedDateTime.class))
 *
 */
public class ModelType implements ParameterizedType {
	private final Type ownerType;
	private final Type rawType;
	private final Type[] actualTypeArguments;

	public ModelType(Type ownerType, Type rawType, Type... parameters) {
		this.ownerType = ownerType;
		this.rawType = rawType;
		this.actualTypeArguments = parameters.clone();
	}

	public static ModelType of(Class<?> rawType, Type... parameters) {
		return new ModelType(null, rawType, parameters);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(actualTypeArguments);
		result = prime * result + Objects.hash(ownerType, rawType);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelType other = (ModelType) obj;
		return Arrays.equals(actualTypeArguments, other.actualTypeArguments)
				&& Objects.equals(ownerType, other.ownerType) && Objects.equals(rawType, other.rawType);
	}

	public Type[] getActualTypeArguments() {
		return actualTypeArguments.clone();
	}

	@Override
	public Type getRawType() {
		return rawType;
	}

	@Override
	public Type getOwnerType() {
		return ownerType;
	}
}
