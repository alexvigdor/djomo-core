/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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
import java.math.BigDecimal;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.internal.CharArraySequence;
import com.bigcloud.djomo.json.SafeString;

public class BigDecimalModel extends BaseModel<BigDecimal> {

	public BigDecimalModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public BigDecimal parse(Parser parser) {
		var chars = parser.parseString();
		if (chars instanceof CharArraySequence cas) {
			return new BigDecimal(cas.buffer, cas.start, cas.len);
		}
		return new BigDecimal(chars.toString());
	}

	@Override
	public void visit(BigDecimal obj, Visitor visitor) {
		visitor.visitString(new SafeString(obj.toString()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public BigDecimal convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		try {
			String p = getParseable(o);
			return new BigDecimal(p);
		} catch (Throwable e) {
			throw new ModelException("Error converting " + o + " to " + type.getTypeName(), e);
		}
	}
}
