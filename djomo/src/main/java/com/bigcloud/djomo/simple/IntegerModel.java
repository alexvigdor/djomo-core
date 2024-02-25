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

public class IntegerModel extends NumberModel<Integer> {

	public IntegerModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public Integer parse(String str) {
		return Integer.valueOf(str);
	}

	@Override
	protected Integer convertNumber(Number n) {
		if (n instanceof Integer i) {
			return i;
		}
		return n.intValue();
	}

	@Override
	protected Integer convertDouble(double value) {
		return (int) value;
	}

	@Override
	protected Integer convertInt(int value) {
		return value;
	}

	@Override
	protected Integer convertLong(long value) {
		return (int) value;
	}

	@Override
	public void visit(Integer obj, Visitor visitor) {
		visitor.visitInt(obj);
	}

	@Override
	public Integer parse(Parser parser) {
		return parser.parseInt();
	}
}
