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

import java.lang.reflect.Array;
import java.util.List;

import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.base.BaseMaker;

public class ArrayMaker<T, I> extends BaseMaker<T, ArrayModel<T, I>> implements ListMaker<T, I> {
	private final List<I> collection;

	public ArrayMaker(ArrayModel<T, I> model, List<I> collection) {
		super(model);
		this.collection = collection;
	}

	@Override
	public void item(I value) {
		collection.add(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T make() {
		Object array = Array.newInstance(model.getType().getComponentType(), collection.size());
		int len = collection.size();
		for (int i = 0; i < len; i++) {
			Array.set(array, i, collection.get(i));
		}
		return (T) array;
	}

}
