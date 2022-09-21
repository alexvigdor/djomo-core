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

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseComplexModel;

public class MapModel<T extends Map<K, V>, K, V> extends BaseComplexModel<T, MapMaker<T, K, V>> implements ObjectModel<T, MapMaker<T, K, V>, MapField<T,K,V>, K, V>{
	final ModelContext context;
	final MethodHandle constructor;
	final Model<K> keyModel;
	final Model<V> valueModel;
	
	public MapModel(Type type, ModelContext context, MethodHandle constructor) {
		super(type, context);
		this.context = context;
		Type valueType = null;
		this.constructor = constructor;
		if (typeArgs != null) {
			Iterator<Type> ti = typeArgs.values().iterator();
			keyModel = (Model<K>) context.get(ti.next());
			valueType = ti.next();
		} else {
			keyModel = null;
		}
		valueModel = (Model<V>) context.get(valueType != null ? valueType : Object.class);
	}

	@Override
	public MapMaker<T, K, V> maker(T obj) {
		var m = maker();
		m.map.putAll(obj);
		return m;
	}

	@Override
	public MapMaker<T, K, V> maker() {
		return new MapMaker<>(this);
	}

	@Override
	public T convert(Object o) {
		if(o==null) {
			return null;
		}
		Model def = context.get(o.getClass());
		if(def instanceof ObjectModel) {
			T dest = newInstance();
			((ObjectModel)def).forEachField(o, (key, val)->{
				if(valueModel!=null) {
					val = valueModel.convert(val);
				}
				if(keyModel != null) {
					key = keyModel.convert(key);
				}
				dest.put((K)key, (V)val);
			});
			return dest;
		}
		throw new RuntimeException("Cannot convert object "+o+" of type "+o.getClass()+" to "+type.getTypeName());
	}

	@Override
	public void forEachField(T t, BiConsumer<K, V> consumer) {
		t.forEach(consumer);
	}
	@Override
	public MapField<T, K, V> getField(CharSequence name) {
		K key;
		if(keyModel!=null) {
			key = keyModel.convert(name);
		}
		else {
			key = (K) name.toString();
		}
		return new MapField<T, K, V>(key, valueModel);
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
		visitor.visitObject(obj, this);
	}

	@Override
	public List<MapField<T, K, V>> fields() {
		return null;
	}

}
