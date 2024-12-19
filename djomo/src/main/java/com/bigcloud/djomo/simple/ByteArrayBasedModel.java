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
import java.util.Base64;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.error.ModelException;

public class ByteArrayBasedModel<T> extends BaseModel<T> {
	final MethodHandle constructor;
	final MethodHandle toByteArray;

	public ByteArrayBasedModel(Type type, ModelContext context, MethodHandle constructor, MethodHandle toByteArray) {
		super(type, context);
		this.constructor = constructor;
		this.toByteArray = toByteArray;
	}

	@Override
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (T) o;
		}
		try {
			return (T) constructor.invoke(Base64.getDecoder().decode(o.toString()));
		} catch (Throwable e) {
			throw new ModelException("Error converting " + o + " to " + type.getTypeName(), e);
		}
	}

	@Override
	public T parse(Parser parser) {
		try {
			return (T) constructor.invoke(Base64.getDecoder().decode(parser.parseString().toString()));
		} catch (Throwable e) {
			throw new ModelException(
					"Error constructing instance of " + type.getName() + " from base64", e);
		}
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		// TODO Auto-generated method stub
		try {
			visitor.visitString(Base64.getEncoder().encodeToString((byte[]) toByteArray.invoke(obj)));
		} catch (Throwable e) {
			throw new ModelException(
					"Error visiting instance of " + type.getName() + " as base64", e);
		}
	}

}
