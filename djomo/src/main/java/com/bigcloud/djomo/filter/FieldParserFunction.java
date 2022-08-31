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
 * Pass in a function that can transform between the JSON source type and target Model Field type.  
 * Useful when default data mapping does not work.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of object that owns the field to be filtered
 * @param <S> The data type found in the Json source
 * @param <F> The data type of the target field in the model
 */
public class FieldParserFunction<T, S, F> extends FieldParserTransform<T, S, F> {
	final Function<S, F> transform;

	public FieldParserFunction(String field, Function<S, F> transform) {
		super(field);
		this.transform = transform;
	}

	public FieldParserFunction(Class<T> type, Class<S> sourceType, Class<F> fieldType,  String field, Function<S, F> transform) {
		super(type, sourceType, fieldType, field);
		this.transform = transform;
	}

	@Override
	public F transform(S in) {
		return transform.apply(in);
	}

}
