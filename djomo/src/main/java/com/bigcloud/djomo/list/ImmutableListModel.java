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

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseListModel;

/**
 * specialized default immutable list implementation, use a Resolver to parse to
 * a different list type
 */
public class ImmutableListModel extends BaseListModel<List> {

	public ImmutableListModel(Type type, ModelContext context, Type valueType) {
		super(type, context, context.get(valueType != null ? valueType : Object.class));
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
			m.tryVisit(i, visitor);
		});
	}

	@Override
	public ImmutableList make(Object maker) {
		return (ImmutableList) maker;
	}

	@Override
	protected void addItem(Object maker, Object item) {
		((ImmutableList)maker).addItem(item);
	}
}
