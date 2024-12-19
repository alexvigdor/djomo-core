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

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class NumberModel<N extends Number> extends BaseModel<N> {

	public NumberModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public N convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (N) o;
		}
		if (o instanceof Number) {
			return convertNumber((Number) o);
		}
		return parse(getParseable(o));
	}

	public N parse(String str) {
		return (N) Double.valueOf(str);
	}

	protected N convertNumber(Number value) {
		return (N) value;
	}

	protected N convertDouble(double value) {
		return (N) Double.valueOf(value);
	}

	protected N convertInt(int value) {
		return (N) Integer.valueOf(value);
	}

	protected N convertLong(long value) {
		return (N) Long.valueOf(value);
	}

	@Override
	public void visit(N obj, Visitor visitor) {
		visitor.visitDouble(obj.doubleValue());
	}

	@Override
	public N parse(Parser parser) {
		double d = parser.parseDouble();
		if(Math.rint(d) == d && Double.isFinite(d)) {
			// Down convert to the simplest representation
			int i = (int) d;
			if(i == d) {
				return (N) (Integer) i;
			}
			return (N) (Long) (long) d;
		}
		return (N) (Double) d;
	}

}
