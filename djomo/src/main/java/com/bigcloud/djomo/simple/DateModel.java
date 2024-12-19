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
import java.util.Date;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

/**
 * This is simple default behavior for legacy java Date objects, treating them
 * as numeric long timestamps. Configure a custom DateFormatModelFactory
 * instead for string formats, or use filters to perform dynamic conversions.
 * 
 * @author Alex Vigdor
 *
 */
public class DateModel extends BaseModel<Date> {
	NumberModel<Long> longModel;

	public DateModel(Type type, ModelContext context) {
		super(type, context);
		longModel = (NumberModel) context.get(Long.class);
	}

	@Override
	public Date convert(Object o) {
		if (o instanceof Date d) {
			return d;
		}
		if (o instanceof Number n) {
			return new Date(n.longValue());
		}
		if (o == null) {
			return null;
		}
		return new Date(longModel.parse(getParseable(o)));
	}

	@Override
	public Date parse(Parser parser) {
		return new Date(parser.parseLong());
	}

	@Override
	public void visit(Date obj, Visitor visitor) {
		visitor.visitLong(obj.getTime());
	}
}
