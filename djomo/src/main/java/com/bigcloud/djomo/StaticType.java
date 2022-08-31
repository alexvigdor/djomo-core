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

/**
 * Provides an easy way to define a complex generic type for parsing, e.g.
 * 
 * Json.read(..., new StaticType&lt;Map&lt;Integer,ZonedDateTime&gt;&gt;() {})
 * 
 * @author Alex Vigdor
 *
 * @param <T>
 */
public abstract class StaticType<T> implements ParameterizedType {
	final ParameterizedType type;

	protected StaticType() {
		type = (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	public Class<T> getStaticType() {
		return (Class<T>) type.getRawType();
	}

	@Override
	public Type[] getActualTypeArguments() {
		return type.getActualTypeArguments();
	}

	@Override
	public Type getRawType() {
		return type.getRawType();
	}

	@Override
	public Type getOwnerType() {
		return type.getOwnerType();
	}

	public int hashCode() {
		return type.hashCode();
	}

	public boolean equals(Object o) {
		return type.equals(o);
	}
}
