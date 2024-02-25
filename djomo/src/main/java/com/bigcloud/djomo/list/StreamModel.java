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

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class StreamModel<T extends Stream> extends BaseComplexModel<T>
	implements ListModel<T> {
	final Model itemModel;
	final Models models;
	
	public StreamModel(Type type, ModelContext context, Type valueType) {
		super(type, context);
		this.itemModel = context.get(valueType != null ? valueType : Object.class);
		this.models = context.models();
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
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		Model<?> def = models.get(o.getClass());
		if (def instanceof ListModel) {
			return (T) ((ListModel) def).stream(o);
		}
		return (T) Stream.of(itemModel.convert(o));
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
	public Model itemModel() {
		return itemModel;
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
		t.forEach(item -> {
			visitor.visitListItem();
			if(item == null) {
				visitor.visitNull();
			}
			else {
				itemModel.visit(item, visitor);
			}
		});
	}
	
	@Override
	public void parseItem(Object listMaker, Parser parser) {
		parser.parseListItem();
		((Stream.Builder)listMaker).add(parser.parse(itemModel));
	}

	@Override
	public T make(Object maker) {
		return (T) ((Stream.Builder)maker).build();
	}
}
