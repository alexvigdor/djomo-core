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
package com.bigcloud.djomo.poly;

import java.lang.reflect.Type;
import java.util.OptionalDouble;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class OptionalDoubleModel extends BaseModel<OptionalDouble> {
	final Model<?> valueModel;

	public OptionalDoubleModel(Type type, ModelContext context) {
		super(type, context);
		valueModel = context.get(double.class);
	}

	@Override
	public OptionalDouble convert(Object o) {
		if (o == null) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of((double) valueModel.convert(o));
	}

	@Override
	public OptionalDouble parse(Parser parser) {
		Object val = valueModel.parse(parser);
		if (val == null) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of((double) val);
	}

	@Override
	public void visit(OptionalDouble obj, Visitor visitor) {
		if (obj.isPresent()) {
			visitor.visit(obj.getAsDouble());
		} else {
			visitor.visit(null);
		}
	}

}
