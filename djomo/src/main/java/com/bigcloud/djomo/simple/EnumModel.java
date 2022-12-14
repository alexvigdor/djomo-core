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
import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.internal.CharSequenceParser;
import com.bigcloud.djomo.io.Buffer;

public class EnumModel<T extends Enum<T>> extends BaseSimpleModel<T> {

	public EnumModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public void print(T obj, Printer printer) {
		printer.quote(obj.name());
	}

	@Override
	public T parse(Buffer input, Buffer overflow) throws IOException {
		return (T) Enum.valueOf(getType(), CharSequenceParser.parse(input, overflow).toString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convert(Object o) {
		if(o==null) {
			return null;
		}
		if(getType().isAssignableFrom(o.getClass())) {
			return (T) o;
		}
		return (T) Enum.valueOf(getType(), o.toString());
	}

}
