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
package com.bigcloud.djomo.filter;

import java.util.function.Function;
/**
 * Pass in a function to transform instances of a given type during visitation.
 * This is useful for performing structural transformations, for example transforming an object or list model down to a plain Json string.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The data type to transform when encountered during visitation
 */
public class TypeVisitorFunction<T> extends TypeVisitorTransform<T> {
	final Function<T, Object> transform;

	public TypeVisitorFunction(Class<T> type, Function<T, Object> transform) {
		super(type);
		this.transform = transform;
	}

	public TypeVisitorFunction(Function<T, Object> transform) {
		super();
		this.transform = transform;
	}

	@Override
	public Object transform(T in) {
		return transform.apply(in);
	}

}
