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

public class ShortModel extends NumberModel<Short> {

	public ShortModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public Short parse(String str) {
		return Short.valueOf(str);
	}

	@Override
	protected Short convertNumber(Number n) {
		return n.shortValue();
	}

	@Override
	protected Short convertDouble(double value) {
		return (short) value;
	}

	@Override
	protected Short convertInt(int value) {
		return (short) value;
	}

	@Override
	protected Short convertLong(long value) {
		return (short) value;
	}

	@Override
	public void visit(Short obj, Visitor visitor) {
		visitor.visitInt(obj.intValue());
	}

	@Override
	public Short parse(Parser parser) {
		return (short) parser.parseInt();
	}
}
