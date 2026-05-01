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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseListModel;

public class CollectionModel<T extends Collection> extends BaseListModel<T> {
	final Supplier<T> constructor;

	public CollectionModel(Type type, ModelContext context, Supplier<T> constructor, Type valueType) {
		super(type, context,  context.get(valueType != null ? valueType : Object.class));
		this.constructor = constructor;
	}

	@Override
	public Object maker(T obj) {
		T maker = maker();
		obj.forEach(maker::add);
		return maker;
	}

	@Override
	public T maker() {
		return constructor.get();
	}

	@Override
	public void forEachItem(T t, Consumer consumer) {
		t.forEach(consumer);
	}

	@Override
	public Stream stream(T t) {
		return t.stream();
	}

	@Override
	public void visitItems(T t, Visitor visitor) {
		var m = itemModel;
		t.forEach(i -> {
			visitor.visitListItem();
			m.tryVisit(i, visitor);
		});
	}
	
	@Override
	public T make(Object maker) {
		return (T) maker;
	}

	@Override
	protected final void addItem(Object maker, Object item) {
		((T)maker).add(item);
	}
}