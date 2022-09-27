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

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseModelFactory;

/**
 * Used to define a custom DateTimeFormatter to be used with java.util.Date
 * objects and/or a specified java.time type
 * 
 * @author Alex Vigdor
 *
 */
public class DateFormatModelFactory extends BaseModelFactory {
	private final DateTimeFormatter formatter;
	private Class<?> type;

	public DateFormatModelFactory(DateTimeFormatter formatter, Class<?> type) {
		this.formatter = formatter;
		this.type = type;
		if (!Date.class.isAssignableFrom(type) && !TemporalAccessor.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException(
					"Can only create DateFormatModelFactory for java.util.Date or java.time.TemporalAccessor implementations, not "
							+ type.getName());
		}
	}

	@Override
	public Model<?> create(Type type, ModelContext context) {
		Class<?> rawType = getRawType(type);
		if (this.type.isAssignableFrom(rawType)) {
			if (Date.class.isAssignableFrom(rawType)) {
				return new DateFormatModel(type, context, formatter);
			} else {
				return new DateTimeFormatterModel(type, context, formatter);
			}
		}
		return null;
	}

}
