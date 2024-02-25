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

public class ByteModel extends NumberModel<Byte> {

	public ByteModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public Byte parse(String str) {
		return Byte.valueOf(str);
	}

	@Override
	protected Byte convertNumber(Number n) {
		return n.byteValue();
	}

	@Override
	protected Byte convertDouble(double value) {
		return (byte) value;
	}

	@Override
	protected Byte convertInt(int value) {
		return (byte) value;
	}

	@Override
	protected Byte convertLong(long value) {
		return (byte) value;
	}

	@Override
	public void visit(Byte obj, Visitor visitor) {
		visitor.visitInt(obj.intValue());
	}

	@Override
	public Byte parse(Parser parser) {
		return (byte) parser.parseInt();
	}

}
