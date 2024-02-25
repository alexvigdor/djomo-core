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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.error.ModelException;

/**
 * Used to provide string formatting for legacy Date objects
 * 
 * @author Alex Vigdor
 *
 */
public class DateFormatModel extends BaseModel<Date> {
	final DateTimeFormatter format;

	public DateFormatModel(Type type, ModelContext context, DateTimeFormatter format) {
		super(type, context);
		this.format = format;
	}

	@Override
	public Date convert(Object o) {
		if (o instanceof Date t) {
			return t;
		}
		if (o == null) {
			return null;
		}
		try {
			String p = getParseable(o);
			return Date.from(format.parse(p, Instant::from));
		} catch (Throwable e) {
			throw new ModelException("Error converting " + o + " to " + type.getTypeName(), e);
		}
	}

	@Override
	public Date parse(Parser parser) {
		return Date.from(format.parse(parser.parseString(), Instant::from));
	}

	@Override
	public void visit(Date obj, Visitor visitor) {
		visitor.visitString(format.format(ZonedDateTime.ofInstant(obj.toInstant(), ZoneId.systemDefault())));
	}
	@Override
	public Format getFormat() {
		return Format.STRING;
	}
}
