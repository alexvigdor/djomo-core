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

import java.util.Collection;

import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.base.BaseMaker;

public class CollectionMaker<T extends Collection<I>, I> extends BaseMaker<T, CollectionModel<T, I>> implements ListMaker<T, I> {
	private final T collection;

	public CollectionMaker(CollectionModel<T, I> model) {
		super(model);
		this.collection = model.newInstance();
	}

	public CollectionMaker(CollectionModel<T, I> model, T collection) {
		super(model);
		this.collection = collection;
	}

	@Override
	public T make() {
		return collection;
	}

	@Override
	public void item(I item) {
		collection.add(item);
	}

}