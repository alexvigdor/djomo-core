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
 * Pass in a function to transform the naturally encountered data type in a Json source into the target model type.
 * This is useful for performing structural transformations, for example transforming a plain Json string into an object or list model.
 * 
 * @author Alex Vigdor
 *
 * @param <I> The natural data type encountered in parsing
 * @param <O> The target data type to transform into
 */
public class TypeParserFunction<I, O> extends TypeParserTransform<I, O> {
	final Function<I, O> transform;

	public TypeParserFunction(Function<I, O> transform) {
		super();
		this.transform = transform;
	}

	public TypeParserFunction(Class<I> in, Class<O> out, Function<I, O> transform) {
		super(in, out);
		this.transform = transform;
	}

	@Override
	public O transform(I in) {
		return transform.apply(in);
	}

}
