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
package com.bigcloud.djomo.list;

import java.lang.reflect.Type;
import java.util.List;
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

/**
 * specialized default immutable list implementation, use a Resolver to parse to
 * a different list type
 */
public class ImmutableListModel extends BaseComplexModel<List> implements ListModel<List> {
	final Model itemModel;
	final Models models;

	public ImmutableListModel(Type type, ModelContext context, Type valueType) {
		super(type, context);
		this.itemModel = context.get(valueType != null ? valueType : Object.class);
		this.models = context.models();
	}

	@Override
	public List convert(Object o) {
		if (o == null) {
			return null;
		}
		Model def = models.get(o.getClass());
		ImmutableList maker = new ImmutableList();
		if (def instanceof ListModel) {
			((ListModel) def).forEachItem(o, i -> maker.addItem(itemModel.convert(i)));
		} else {
			maker.addItem(itemModel.convert(o));
		}
		return maker;
	}

	@Override
	public void visit(List obj, Visitor visitor) {
		visitor.visitList(obj, this);
	}

	@Override
	public List parse(Parser parser) {
		return (List) parser.parseList(this);
	}

	@Override
	public Format getFormat() {
		return Format.LIST;
	}

	@Override
	public void forEachItem(List t, Consumer consumer) {
		t.forEach(consumer);
	}

	@Override
	public Stream stream(List t) {
		return t.stream();
	}

	@Override
	public Model itemModel() {
		return itemModel;
	}

	@Override
	public Object maker(List obj) {
		var maker = new ImmutableList();
		obj.forEach(maker::addItem);
		return maker;
	}

	@Override
	public Object maker() {
		return new ImmutableList();
	}

	@Override
	public void visitItems(List t, Visitor visitor) {
		final Model m = itemModel;
		t.forEach(i -> {
			visitor.visitListItem();
			if (i == null) {
				visitor.visitNull();
			} else {
				m.visit(i, visitor);
			}
		});
	}

	@Override
	public void parseItem(Object listMaker, Parser parser) {
		parser.parseListItem();
		((ImmutableList) listMaker).addItem(parser.parse(itemModel));
	}

	@Override
	public ImmutableList make(Object maker) {
		return (ImmutableList) maker;
	}
}
