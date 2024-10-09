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
package com.bigcloud.djomo.base;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bigcloud.djomo.ModelType;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.annotation.Order;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.internal.CharSequenceLookup;
import com.bigcloud.djomo.object.BeanField;

/**
 * Common baseline behavior for Object Models with a predefined set of fields
 * 
 * @author Alex Vigdor
 *
 * @param <T>
 */
public abstract class BaseObjectModel<T> extends BaseComplexModel<T> implements ObjectModel<T> {
	protected final CharSequenceLookup<Field> fields;
	protected final Field[] sortedFields;
	protected final List<Field> fieldList;

	public BaseObjectModel(Type type, ModelContext context) throws IllegalAccessException {
		super(type, context);
		var fieldMap = initFields(context);
		this.fields = new CharSequenceLookup<>(fieldMap);
		@SuppressWarnings("unchecked")
		Field[] sortedFields = fieldMap.values().toArray((Field[]) new Field[0]);
		Order order = this.type.getAnnotation(Order.class);
		if (order != null && order.value() != null && order.value().length > 0) {
			final String[] declared = order.value();
			final int dl = declared.length;
			Arrays.sort(sortedFields, (a, b) -> {
				int p1 = dl;
				int p2 = dl;
				for (int i = 0; i < dl; i++) {
					String d = declared[i];
					if (a.key().toString().equals(d)) {
						p1 = i;
					}
					if (b.key().toString().equals(d)) {
						p2 = i;
					}
					if (p1 < dl && p2 < dl) {
						break;
					}
				}
				if (p1 == p2) {
					return a.key().toString().compareTo(b.key().toString());
				}
				return p1 - p2;
			});
		} else {
			Arrays.sort(sortedFields, (a, b) -> a.key().toString().compareTo(b.key().toString()));
		}
		this.sortedFields = sortedFields;
		this.fieldList = List.of(sortedFields);
	}

	protected BaseObjectModel(Models models, Type type, Field... fields) {
		super(type, models);
		this.sortedFields = fields;
		this.fieldList = List.of(sortedFields);
		this.fields = new CharSequenceLookup<Field>(
				fieldList.stream().collect(Collectors.toMap(f -> f.key().toString(), Function.identity())));
	}

	protected abstract Map<CharSequence, Field> initFields(ModelContext context) throws IllegalAccessException;

	@Override
	public Object maker(T obj) {
		var m = maker();
		for (Field f : sortedFields) {
			f.set(m, f.get(obj));
		}
		return m;
	}

	@Override
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (T) o;
		}
		InstanceParser mp = new InstanceParser(models, o);
		return mp.filter(this);
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitObject(obj, this);
	}

	@Override
	public void tryVisit(T obj, Visitor visitor) {
		if(obj == null) {
			visitor.visitNull();
			return;
		}
		Class c = obj.getClass();
		if(c == type) {
			visitor.visitObject(obj, this);
		}
		else {
			((Model)models.get(c)).visit(obj, visitor);
		}
	}

	@Override
	public T parse(Parser parser) {
		return (T) parser.parseObject(this);
	}

	@Override
	public void forEachField(T t, BiConsumer consumer) {
		for (Field f : sortedFields) {
			consumer.accept(f.key(), f.get(t));
		}
	}

	@Override
	public void visitFields(T t, Visitor visitor) {
		for (Field f : sortedFields) {
			f.visit(t, visitor);
		}
	}

	@Override
	public Field getField(CharSequence name) {
		return fields.get(name);
	}

	@Override
	public List<Field> fields() {
		return fieldList;
	}

	protected <CLASS_TYPE> void accessor(MethodHandles.Lookup lookup, ModelContext context, BeanField.Builder fieldDef,
			Method method, Map<String, Type> typeArgs) throws IllegalAccessException {
		Type ft = fixGenericType(method.getGenericReturnType(), typeArgs);
		fieldDef.accessor(lookup.unreflect(method));
		fieldDef.model((Model) context.get(ft));
	}

	protected <CLASS_TYPE> void publicField(MethodHandles.Lookup lookup, ModelContext context,
			BeanField.Builder fieldDef, java.lang.reflect.Field field, Map<String, Type> typeArgs)
			throws IllegalAccessException {
		Type ft = fixGenericType(field.getGenericType(), typeArgs);
		fieldDef.accessor(lookup.unreflectGetter(field));
		fieldDef.mutator(lookup.unreflectSetter(field));
		fieldDef.model((Model) context.get(ft));
	}

	protected <CLASS_TYPE> void mutator(MethodHandles.Lookup lookup, ModelContext context, BeanField.Builder fieldDef,
			Method method, Map<String, Type> typeArgs) throws IllegalAccessException {
		Type ft = fixGenericType(method.getGenericParameterTypes()[0], typeArgs);
		fieldDef.mutator(lookup.unreflect(method));
		fieldDef.model((Model) context.get(ft));
	}

	protected <CLASS_TYPE> void mutator(MethodHandles.Lookup lookup, ModelContext context, BeanField.Builder fieldDef,
			MethodHandle method, Type fieldType, Map<String, Type> typeArgs) throws IllegalAccessException {
		fieldType = fixGenericType(fieldType, typeArgs);
		fieldDef.mutator(method);
		fieldDef.model((Model) context.get(fieldType));
	}

	private Type fixGenericType(Type declared, Map<String, Type> args) {
		Type ft = declared;
		if (args != null) {
			if (ft instanceof TypeVariable<?>) {
				if (args.containsKey(ft.getTypeName())) {
					ft = args.get(ft.getTypeName());
				}
			} else if (ft instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) ft;
				Type[] ts = pt.getActualTypeArguments();
				for (int i = 0; i < ts.length; i++) {
					Type t = ts[i];
					if (args.containsKey(t.getTypeName())) {
						ts[i] = args.get(t.getTypeName());
					}
				}
				ft = new ModelType(pt.getOwnerType(), pt.getRawType(), ts);
			}
		}
		return ft;
	}

	@Override
	public Format getFormat() {
		return Format.OBJECT;
	}
}
