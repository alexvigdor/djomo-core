/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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
package com.bigcloud.djomo.base;

import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;

public abstract class BaseListModel<T> extends BaseComplexModel<T> implements ListModel<T> {
	final protected Model itemModel;

	public BaseListModel(Type type, ModelContext context, Model itemModel) {
		super(type, context);
		this.itemModel = itemModel;
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
	public Model itemModel() {
		return itemModel;
	}

	@Override
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (T) o;
		}
		Model<?> def = models.get(o.getClass());
		Object maker = maker();
		if (def instanceof ListModel) {
			((ListModel) def).forEachItem(o, i -> addItem(maker, itemModel.convert(i)));
		} else {
			addItem(maker, itemModel.convert(o));
		}
		return make(maker);
	}

	@Override
	public void parseItem(Object listMaker, Parser parser) {
		parser.parseListItem();
		addItem(listMaker, parser.parse(itemModel));
	}
	
	@Override
	public void tryVisit(T obj, Visitor visitor) {
		if(obj == null) {
			visitor.visitNull();
		}
		else {
			visitor.visitList(obj, this);
		}
	}

	protected abstract void addItem(Object maker, Object item);
}
