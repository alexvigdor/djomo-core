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

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
/**
 * Base ListModel for filtering other list models.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The list or array type to filter
 * @param <M> The type of ListMaker used
 * @param <I> The item model for this list model
 */
public class FilterListModel<T, M extends ListMaker<T, I>, I> extends FilterModel<T> implements ListModel<T, M, I> {
	final ListModel<T, M, I> listModel;

	public FilterListModel(ListModel<T, M, I> delegate) {
		super(delegate);
		this.listModel = delegate;
	}

	@Override
	public M maker(T obj) {
		return listModel.maker(obj);
	}

	@Override
	public M maker() {
		return listModel.maker();
	}

	@Override
	public void forEachItem(T t, Consumer<I> consumer) {
		listModel.forEachItem(t, consumer);
	}

	@Override
	public Stream<I> stream(T t) {
		return listModel.stream(t);
	}

	@Override
	public Model<I> itemModel() {
		return listModel.itemModel();
	}

}
