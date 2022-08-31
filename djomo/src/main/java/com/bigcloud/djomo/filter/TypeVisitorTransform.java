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

import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Define a method to transform instances of a given type during visitation.
 * This is useful for performing structural transformations, for example transforming an object or list model down to a plain Json string.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The data type to transform when encountered during visitation
 */
public abstract class TypeVisitorTransform<T> extends FilterVisitor {
	final Class<T> type;

	public abstract Object transform(T in);

	public TypeVisitorTransform(Class<T> type) {
		this.type = type;
	}

	public TypeVisitorTransform() {
		type = ConcreteType.get(this.getClass(), 0);
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public void visit(Object obj) {
		if (obj != null && type.isInstance(obj)) {
			obj = transform((T) obj);
		}
		visitor.visit(obj);
	}
}
