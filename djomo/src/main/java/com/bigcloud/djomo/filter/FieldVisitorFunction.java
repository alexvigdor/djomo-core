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
 * Pass in a function that can transform the value of a Model Field when the field name and type match.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of object that owns the field to be filtered
 * @param <F> The data type of the source field in the model
 */
public class FieldVisitorFunction<T, F> extends FieldVisitorTransform<T, F> {
	final Function<F, Object> transform;

	public FieldVisitorFunction(String field, Function<F, Object> transform) {
		super(field);
		this.transform = transform;
	}

	public FieldVisitorFunction(Class<T> type, Class<F> fieldType, String field, Function<F, Object> transform) {
		super(type, fieldType, field);
		this.transform = transform;
	}

	@Override
	public Object transform(F in) {
		return transform.apply(in);
	}

}
