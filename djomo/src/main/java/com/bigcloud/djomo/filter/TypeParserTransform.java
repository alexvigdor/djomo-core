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

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Define a method to transform the naturally encountered data type in a Json source into the target model type.
 * This is useful for performing structural transformations, for example transforming a plain Json string into an object or list model.
 * 
 * @author Alex Vigdor
 *
 * @param <I> The natural data type encountered in parsing
 * @param <O> The target data type to transform into
 */
public abstract class TypeParserTransform<I, O> extends FilterParser {
	final Class<O> to;
	final Class<I> from;

	public abstract O transform(I in);

	public TypeParserTransform() {
		to = ConcreteType.get(this.getClass(), 1);
		from = ConcreteType.get(this.getClass(), 0);
	}

	public TypeParserTransform(Class<I> in, Class<O> out) {
		to = out;
		from = in;
	}

	public Class<O> getType() {
		return to;
	}

	public Class<I> getFromType() {
		return from;
	}

	@Override
	public <T> T parse(Model<T> model) {
		Object obj = parser.parse(model);
		if (obj != null && from.isInstance(obj) && to.isAssignableFrom(model.getType())) {
			obj = transform((I) obj);
		}
		return (T) obj;
	}
}
