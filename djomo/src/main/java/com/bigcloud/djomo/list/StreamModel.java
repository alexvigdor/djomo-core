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
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class StreamModel<T extends Stream<I>, I> extends BaseComplexModel<T, StreamMaker<T, I>>
	implements ListModel<T, StreamMaker<T, I>, I> {
	final Model<I> itemModel;
	final Models models;
	
	public StreamModel(Type type, ModelContext context, Type valueType) {
		super(type, context);
		this.itemModel = (Model<I>) context.get(valueType != null ? valueType : Object.class);
		this.models = context.models();
	}

	@Override
	public StreamMaker<T, I> maker(T obj) {
		return new StreamMaker<>(this, obj);
	}

	@Override
	public StreamMaker<T, I> maker() {
		return new StreamMaker<>(this);
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
		if(def instanceof ListModel) {
			return (T) ((ListModel)def).stream(o);
		}
		throw new RuntimeException(
				"Cannot convert object " + o + " of type " + o.getClass() + " to " + type.getTypeName());
	}

	@Override
	public void forEachItem(T t, Consumer<I> consumer) {
		t.forEach(consumer);
	}

	@Override
	public Stream<I> stream(T t) {
		return t;
	}

	@Override
	public Model<I> itemModel() {
		return itemModel;
	} 
	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitList(obj, this);
	}
}
