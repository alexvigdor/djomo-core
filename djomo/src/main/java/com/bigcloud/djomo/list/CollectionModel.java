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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class CollectionModel<T extends Collection<I>, I> extends BaseComplexModel<T, CollectionMaker<T, I>>
		implements ListModel<T, CollectionMaker<T, I>, I> {
	final MethodHandle constructor;
	final Models models;
	final Model<I> itemModel;

	public CollectionModel(Type type, ModelContext context, MethodHandle constructor, Type valueType) {
		super(type, context);
		this.constructor = constructor;
		this.models = context.models();
		this.itemModel = (Model<I>) context.get(valueType != null ? valueType : Object.class);
	}

	@Override
	public CollectionMaker<T, I> maker(T obj) {
		var maker = maker();
		obj.forEach(maker::item);
		return maker;
	}

	@Override
	public CollectionMaker<T, I> maker() {
		return new CollectionMaker<>(this);
	}

	@Override
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (T) o;
		}
		Model def = models.get(o.getClass());
		CollectionMaker<T, I> maker = maker();
		if (def instanceof ListModel) {
			((ListModel) def).forEachItem(o, i -> maker.item(itemModel.convert(i)));
		}
		else {
			maker.item(itemModel.convert(o));
		}
		return maker.make();
	}

	@Override
	public void forEachItem(T t, Consumer<I> consumer) {
		t.forEach(consumer);
	}

	@Override
	public Stream<I> stream(T t) {
		return t.stream();
	}

	@Override
	public Model<I> itemModel() {
		return itemModel;
	}

	public T newInstance() {
		try {
			var c = constructor;
			if(c == null) {
				throw new RuntimeException("No constructor for "+type);
			}
			return (T) c.invoke();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitList(obj, this);
	}
}