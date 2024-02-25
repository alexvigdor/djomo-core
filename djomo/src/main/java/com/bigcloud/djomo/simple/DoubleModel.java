/*******************************************************************************
 * Copyright 2022 Alex Vigdor
 * 
 * Copyright 2018 Ulf Adams
 * 	- portions derived from "ryu" library also licensed under Apache 2.0
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

/**
 * The double printing code here is borrowed from the "ryu" library.
 * https://github.com/ulfjack/ryu
 * 
 * @author Alex Vigdor
 * @author Ulf Adams
 *
 */
public class DoubleModel extends NumberModel<Double> {


	public DoubleModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public Double parse(String str) {
		return Double.valueOf(str);
	}

	@Override
	protected Double convertNumber(Number n) {
		if (n instanceof Double d) {
			return d;
		}
		return n.doubleValue();
	}

	@Override
	protected Double convertDouble(double value) {
		return value;
	}

	@Override
	protected Double convertInt(int value) {
		return (double) value;
	}

	@Override
	protected Double convertLong(long value) {
		return (double) value;
	}

	@Override
	public void visit(Double obj, Visitor visitor) {
		visitor.visitDouble(obj);
	}

	@Override
	public Double parse(Parser parser) {
		return parser.parseDouble();
	}
}
