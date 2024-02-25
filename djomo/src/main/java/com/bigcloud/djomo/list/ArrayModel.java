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

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ArrayModel<T> extends BaseComplexModel<T> implements ListModel<T>{
	final Class<?> componentType;
	final Models models;
	final Model itemModel;

	public ArrayModel(Type type, ModelContext context) {
		super(type, context);
		this.componentType = getType().getComponentType();
		this.models = context.models();
		this.itemModel = context.get(componentType);
	}

	@Override
	public Object maker(T obj) {
		ArrayList start = new ArrayList();
		int len = Array.getLength(obj);
		for(int i=0; i<len;i++) {
			start.add(Array.get(obj,  i));
		}
		return start;
	}

	@Override
	public Object maker() {
		return new ArrayList();
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
		List maker = (List) maker();
		if (def instanceof ListModel) {
			((ListModel) def).forEachItem(o, i -> maker.add(itemModel.convert(i)));
		} 
		else {
			maker.add(itemModel.convert(o));
		}
		return make(maker);
	}

	@Override
	public void forEachItem(T t, Consumer consumer) {
		int len = Array.getLength(t);
		for (int i = 0; i < len; i++) {
			consumer.accept( Array.get(t, i));
		}
	}

	@Override
	public Stream stream(T t) {
		if(type == double[].class) {
			return Arrays.stream((double[]) t).boxed();
		}
		if(type == long[].class) {
			return Arrays.stream((long[]) t).boxed();
		}
		if(type == int[].class) {
			return Arrays.stream((int[]) t).boxed();
		}
		return Arrays.stream((Object[])t);
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
		var m = itemModel;
		int len = Array.getLength(t);
		for (int i = 0; i < len; i++) {
			visitor.visitListItem();
			var o = Array.get(t, i);
			if(o == null) {
				visitor.visitNull();
			}
			else {
				m.visit(o, visitor);
			}
		}
	}

	@Override
	public void parseItem(Object listMaker, Parser parser) {
		// TODO Auto-generated method stub
		parser.parseListItem();
		((List)listMaker).add(parser.parse(itemModel));
	}

	@Override
	public T make(Object maker) {
		List list = (List) maker;
		int len = list.size();
		Object array = Array.newInstance(componentType, len);
		for (int i = 0; i < len; i++) {
			Array.set(array, i, list.get(i));
		}
		return (T) array;
	}
}
