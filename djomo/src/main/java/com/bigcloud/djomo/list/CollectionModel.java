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
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class CollectionModel<T extends Collection> extends BaseComplexModel<T>
		implements ListModel<T> {
	final MethodHandle constructor;
	final Models models;
	final Model itemModel;

	public CollectionModel(Type type, ModelContext context, MethodHandle constructor, Type valueType) {
		super(type, context);
		this.constructor = constructor;
		this.models = context.models();
		this.itemModel = context.get(valueType != null ? valueType : Object.class);
	}

	@Override
	public Object maker(T obj) {
		Collection maker = (Collection) maker();
		obj.forEach(maker::add);
		return maker;
	}

	@Override
	public Object maker() {
		return newInstance();
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
		T maker = (T) maker();
		if (def instanceof ListModel) {
			((ListModel) def).forEachItem(o, i -> maker.add(itemModel.convert(i)));
		}
		else {
			maker.add(itemModel.convert(o));
		}
		return maker;
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
	public Model itemModel() {
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
	@Override
	public T parse(Parser parser) {
		return (T) parser.parseList(this);
	}
	@Override
	public Format getFormat() {
		return Format.LIST;
	}

	@Override
	public void visitItems(T t, Visitor visitor) {
		var m = itemModel;
		t.forEach(i -> {
			visitor.visitListItem();
			if(i == null) {
				visitor.visitNull();
			}
			else {
				m.visit(i, visitor);
			}
		});
	}
	
	@Override
	public void parseItem(Object listMaker, Parser parser) {
		parser.parseListItem();
		((Collection)listMaker).add(parser.parse(itemModel));
	}

	@Override
	public T make(Object maker) {
		return (T) maker;
	}
}