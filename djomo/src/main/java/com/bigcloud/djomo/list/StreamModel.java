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
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseListModel;

public class StreamModel<T extends Stream> extends BaseListModel<T> {
	
	public StreamModel(Type type, ModelContext context, Type valueType) {
		super(type, context, context.get(valueType != null ? valueType : Object.class));
	}

	@Override
	public Object maker(T obj) {
		var builder = Stream.builder();
		obj.forEach(builder::accept);
		return builder;
	}

	@Override
	public Object maker() {
		return Stream.builder();
	}

	@Override
	public void forEachItem(T t, Consumer consumer) {
		t.forEach(consumer);
	}

	@Override
	public Stream stream(T t) {
		return t;
	}

	@Override
	public void visitItems(T t, Visitor visitor) {
		Model m = itemModel;
		t.forEach(item -> {
			visitor.visitListItem();
			m.tryVisit(item, visitor);
		});
	}

	@Override
	public T make(Object maker) {
		return (T) ((Stream.Builder)maker).build();
	}

	@Override
	protected void addItem(Object maker, Object item) {
		((Stream.Builder)maker).add(item);
	}
}
