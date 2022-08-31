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
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class ArrayModel<T, I> extends BaseComplexModel<T, ArrayMaker<T, I>> implements ListModel<T, ArrayMaker<T, I>, I>{
	final Class<?> componentType;
	final Models models;
	final Model<I> itemModel;

	public ArrayModel(Type type, ModelContext context) {
		super(type, context);
		this.componentType = getType().getComponentType();
		this.models = context.models();
		this.itemModel = (Model<I>) context.get(componentType);
	}

	@Override
	public ArrayMaker<T, I> maker(T obj) {
		ArrayList<I> start = new ArrayList<I>();
		int len = Array.getLength(obj);
		for(int i=0; i<len;i++) {
			start.add((I)Array.get(obj,  i));
		}
		return new ArrayMaker<T, I>(this, start);
	}

	@Override
	public ArrayMaker<T, I> maker() {
		return new ArrayMaker<>(this, new ArrayList<>());
	}

	@Override
	public T convert(Object o) {
		if(o==null) {
			return null;
		}
		if(o.getClass() == getType()) {
			return (T) o;
		}
		Model<?> def = models.get(o.getClass());
		if(def instanceof ListModel) {
			ArrayMaker<T, I> maker = maker();
			((ListModel)def).forEachItem(o, i->maker.item((I) i));
			return maker.make();
		}
		else if(def.getType() == componentType) {
			ArrayMaker<T, I> maker = maker();
			maker.item((I) o);
			return maker.make();
		}
		throw new RuntimeException("Cannot convert object "+o+" of type "+o.getClass()+" to "+type.getTypeName());
	}

	@Override
	public void forEachItem(T t, Consumer<I> consumer) {
		int len = Array.getLength(t);
		for (int i = 0; i < len; i++) {
			consumer.accept((I) Array.get(t, i));
		}
	}

	@Override
	public Stream<I> stream(T t) {
		if(type == double[].class) {
			return (Stream<I>) Arrays.stream((double[]) t).boxed();
		}
		if(type == long[].class) {
			return (Stream<I>) Arrays.stream((long[]) t).boxed();
		}
		if(type == int[].class) {
			return (Stream<I>) Arrays.stream((int[]) t).boxed();
		}
		return (Stream<I>) Arrays.stream((Object[])t);
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
