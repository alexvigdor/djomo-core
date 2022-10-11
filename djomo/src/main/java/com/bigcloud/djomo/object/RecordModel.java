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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.object.BeanField.Builder;

public class RecordModel<T>
		extends ObjectMethodsModel<T, RecordMaker<T>> {
	final MethodHandle constructor;
	final Object[] args;

	public RecordModel(Type type, ModelContext context,
			MethodHandle constructor) throws IllegalAccessException {
		super(type, context);
		this.constructor = constructor;
		this.args = new Object[getType().getRecordComponents().length];
	}

	@Override
	public RecordMaker<T> maker() {
		return new RecordMaker<>(this);
	}

	public Object[] newArgs() {
		return args.clone();
	}

	@SuppressWarnings("unchecked")
	public T create(Object[] args) {
		try {
			return (T) constructor.invokeWithArguments(args);
		} catch (Throwable e) {
			throw new RuntimeException(
					"Unable to create instance of " + getType().getName() + " with " + Arrays.toString(args), e);
		}
	}

	@Override
	protected Map<CharSequence, BeanField<T, Object>> initFields(ModelContext context) throws IllegalAccessException {
		Lookup lookup = MethodHandles.lookup();
		var fields = new ConcurrentHashMap<String, BeanField.Builder<T,Object>>();
		Function<String, BeanField.Builder<T,Object>> fieldLookup = (name) -> fields.computeIfAbsent(name,
				n -> BeanField.<T, Object>builder().name(n));
		processMethods(lookup, context, fieldLookup);
		var rcs = type.getRecordComponents();
		for(int i=0; i<rcs.length;i++) {
			var rc = rcs[i];
			var ac = rc.getAccessor();
			if(ac.trySetAccessible()) {
				accessor(lookup, context, fieldLookup.apply(rc.getName()), ac, typeArgs);
			}
			// our mutator is an array accessor prebound to the right index
			mutator(lookup, context, fieldLookup.apply(rc.getName()),
					MethodHandles.insertArguments(MethodHandles.arrayElementSetter(Object[].class),1,i), rc.getGenericType(), typeArgs);
		}
		return fields.entrySet().stream()
				.map(e -> new AbstractMap.SimpleEntry<String, BeanField>(e.getKey(), e.getValue().build()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	protected void processMethod(Method method, Lookup lookup, ModelContext context,
			Function<String, Builder<T, Object>> fieldLookup) throws IllegalAccessException {
		// no further processing needed, parent class will handle basic accessors
	}

}