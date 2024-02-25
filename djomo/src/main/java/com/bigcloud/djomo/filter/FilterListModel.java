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

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
/**
 * Base ListModel for filtering other list models.
 * 
 * @author Alex Vigdor
 *
 * @param <T> The list or array type to filter
 */
public class FilterListModel<T> extends FilterModel<T> implements ListModel<T> {
	protected final ListModel<T> listModel;

	public FilterListModel(ListModel<T> delegate) {
		super(delegate);
		this.listModel = delegate;
	}

	@Override
	public Object maker(T obj) {
		return listModel.maker(obj);
	}

	@Override
	public Object maker() {
		return listModel.maker();
	}

	@Override
	public void forEachItem(T t, Consumer consumer) {
		listModel.forEachItem(t, consumer);
	}

	@Override
	public Stream stream(T t) {
		return listModel.stream(t);
	}

	@Override
	public Model itemModel() {
		return listModel.itemModel();
	}

	@Override
	public void visitItems(T t, Visitor visitor) {
		listModel.visitItems(t, visitor);
	}

	@Override
	public void parseItem(Object listMaker, Parser parser) {
		listModel.parseItem(listMaker, parser);
	}

	@Override
	public T make(Object maker) {
		return listModel.make(maker);
	}
	@Override
	public T parse(Parser parser) {
		return (T) parser.parseList(this);
	}

}
