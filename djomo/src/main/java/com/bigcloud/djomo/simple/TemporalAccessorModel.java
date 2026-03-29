/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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
package com.bigcloud.djomo.simple;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.temporal.TemporalAccessor;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.TemporalType;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.error.ModelException;

public class TemporalAccessorModel<T extends TemporalAccessor> extends BaseModel<T> {

	final MethodHandle fromHandle;
	final MethodHandle parseHandle;
	final TemporalType<T> temporalType;

	public TemporalAccessorModel(Type type, ModelContext context) {
		super(type, context);
		temporalType = TemporalType.typeOf(this.type);
		MethodHandle fromHandle;
		MethodHandle parseHandle;
		try {
			Method method = this.type.getDeclaredMethod("from", TemporalAccessor.class);
			fromHandle = MethodHandles.lookup().unreflect(method);
			Method parseMethod = this.type.getDeclaredMethod("parse", CharSequence.class);
			parseHandle = MethodHandles.lookup().unreflect(parseMethod);

		} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
			// this just limits conversion capabilities
			fromHandle = null;
			parseHandle = null;
		}
		this.fromHandle = fromHandle;
		this.parseHandle = parseHandle;
	}

	@Override
	public T convert(Object t) {
		if (t instanceof TemporalAccessor ta) {
			if (fromHandle == null) {
				return (T) ta;
			}
			try {
				return (T) fromHandle.invoke(ta);
			} catch (Throwable e) {
				throw new ModelException("Unable to convert TemporalAccessor " + ta + " of type "
						+ ta.getClass().getName() + " to " + type.getTypeName(), e);
			}
		}
		if (t == null) {
			return null;
		}
		if (parseHandle != null) {
			try {
				String p = getParseable(t);
				return (T) parseHandle.invoke(p);
			} catch (Throwable e) {
				throw new ModelException("Error converting " + t + " to " + type.getTypeName(), e);
			}
		}
		return null;
	}

	@Override
	public T parse(Parser parser) {
		return parser.parseTemporal(temporalType);
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitTemporal(obj);
	}

}
