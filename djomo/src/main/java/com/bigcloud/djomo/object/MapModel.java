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
package com.bigcloud.djomo.object;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class MapModel<T extends Map> extends BaseComplexModel<T> implements ObjectModel<T> {
	final ModelContext context;
	final MethodHandle constructor;
	final Model keyModel;
	final Model valueModel;

	public MapModel(Type type, ModelContext context, MethodHandle constructor) {
		super(type, context);
		this.context = context;
		Type valueType = null;
		this.constructor = constructor;
		if (typeArgs != null) {
			Iterator<Type> ti = typeArgs.values().iterator();
			keyModel = context.get(ti.next());
			valueType = ti.next();
		} else {
			keyModel = null;
		}
		valueModel = context.get(valueType != null ? valueType : Object.class);
	}

	@Override
	public Object maker(T obj) {
		var m = newInstance();
		m.putAll(obj);
		return m;
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
		Model def = context.get(o.getClass());
		if (def instanceof ObjectModel) {
			T dest = newInstance();
			((ObjectModel) def).forEachField(o, (key, val) -> {
				if (valueModel != null) {
					val = valueModel.convert(val);
				}
				if (keyModel != null) {
					key = keyModel.convert(key);
				}
				dest.put(key, val);
			});
			return dest;
		}
		throw new RuntimeException(
				"Cannot convert object " + o + " of type " + o.getClass() + " to " + type.getTypeName());
	}

	@Override
	public void forEachField(T t, BiConsumer consumer) {
		t.forEach(consumer);
	}

	@Override
	public Field getField(CharSequence name) {
		Object key;
		if (keyModel != null) {
			key = keyModel.convert(name);
		} else {
			key = name.toString();
		}
		return new MapField(key, valueModel);
	}

	public T newInstance() {
		try {
			var c = constructor;
			if (c == null) {
				throw new RuntimeException("No constructor for " + type);
			}
			return (T) c.invoke();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitObject(obj, this);
	}

	@Override
	public T parse(Parser parser) {
		return (T) parser.parseObject(this);
	}

	@Override
	public List<Field> fields() {
		return null;
	}

	@Override
	public Format getFormat() {
		return Format.OBJECT;
	}

	@Override
	public void visitFields(T t, Visitor visitor) {
		for (Map.Entry entry : ((Map<?, ?>) t).entrySet()) {
			visitor.visitObjectField(entry.getKey());
			Object val = entry.getValue();
			if (val == null) {
				visitor.visitNull();
			} else {
				valueModel.visit(entry.getValue(), visitor);
			}
		}
	}

	@Override
	public T make(Object maker) {
		return (T) maker;
	}
}
