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
package com.bigcloud.djomo.simple;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.error.ModelException;

public class StringBasedModel<T> extends BaseModel<T> {
	final MethodHandle constructor;
	final MethodHandle toString;

	public StringBasedModel(Type type, ModelContext context, MethodHandle constructor, MethodHandle toString) {
		super(type, context);
		this.constructor = constructor;
		this.toString = toString;
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		try {
			visitor.visitString((CharSequence) toString.invoke(obj));
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T parse(Parser parser) {
		try {
			return (T) constructor.invoke(parser.parseString().toString());
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == type) {
			return (T) o;
		}
		try {
			String p = getParseable(o);
			return (T) constructor.invoke(p);
		} catch (Throwable e) {
			throw new ModelException("Error converting " + o + " to " + type.getTypeName(), e);
		}
	}

}
