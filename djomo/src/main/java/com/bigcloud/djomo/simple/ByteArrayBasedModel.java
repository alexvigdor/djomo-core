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

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.io.Buffer;

public class ByteArrayBasedModel<T> extends BaseSimpleModel<T> {
	final SimpleModel<byte[]> byteArrayModel;
	final MethodHandle constructor;
	final MethodHandle toByteArray;

	public ByteArrayBasedModel(Type type, ModelContext context, MethodHandle constructor, MethodHandle toByteArray) {
		super(type, context);
		byteArrayModel = (SimpleModel<byte[]>) context.<byte[]>get(byte[].class);
		this.constructor = constructor;
		this.toByteArray = toByteArray;
	}

	@Override
	public void print(T obj, Printer printer) {
		try {
			byteArrayModel.print((byte[]) toByteArray.invoke(obj), printer);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T parse(Buffer input, Buffer overflow) throws IOException {
		byte[] bytes = byteArrayModel.parse(input, overflow);
		try {
			return (T) constructor.invoke(bytes);
		} catch (Throwable e) {
			throw new ModelException(
					"Error constructing instance of " + type.getName() + " from byte array length " + bytes.length, e);
		}
	}

	@Override
	public T convert(Object o) {
		byte[] bytes = byteArrayModel.convert(o);
		if (bytes == null) {
			return null;
		}
		try {
			return (T) constructor.invoke(bytes);
		} catch (Throwable e) {
			throw new ModelException("Error converting " + o + " to " + type.getTypeName(), e);
		}
	}

}
