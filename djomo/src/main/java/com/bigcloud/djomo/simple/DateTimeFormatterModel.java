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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.error.ModelException;

/**
 * Used to provide customized string formatting for java.time objects
 * 
 * @author Alex Vigdor
 *
 */
public class DateTimeFormatterModel extends BaseModel<TemporalAccessor> {
	final DateTimeFormatter format;
	final MethodHandle fromHandle;

	public DateTimeFormatterModel(Type type, ModelContext context, DateTimeFormatter format) {
		super(type, context);
		this.format = format;
		try {
			Method method = this.type.getDeclaredMethod("from", TemporalAccessor.class);
			fromHandle = MethodHandles.lookup().unreflect(method);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
			throw new ModelException("Unable to create formatter model for type " + type.getTypeName(), e);
		}
	}

	@Override
	public TemporalAccessor convert(Object o) {
		if (o instanceof TemporalAccessor t) {
			try {
				return (TemporalAccessor) fromHandle.invoke(t);
			} catch (Throwable e) {
				throw new ModelException("Unable to convert TemporalAccessor " + t + " of type "
						+ t.getClass().getName() + " to " + type.getTypeName(), e);
			}
		}
		if (o == null) {
			return null;
		}
		try {
			String p = getParseable(o);
			return format.parse(p, this::convert);
		} catch (Throwable e) {
			throw new ModelException("Error converting " + o + " to " + type.getTypeName(), e);
		}
	}

	@Override
	public TemporalAccessor parse(Parser parser) {
		return format.parse(parser.parseString(), this::convert);
	}

	@Override
	public void visit(TemporalAccessor obj, Visitor visitor) {
		visitor.visitString(format.format(obj));
	}

}
