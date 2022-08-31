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
import com.bigcloud.djomo.api.Printer;

public class DoubleModel extends NumberModel<Double>{

	public DoubleModel(Type type, ModelContext context) {
		super(type, context);
	}
	
	@Override
	public Double parse(String str) {
		return Double.valueOf(str);
	}

	@Override
	protected Double convertNumber(Number n) {
		return n.doubleValue();
	}

	@Override
	public void print(Double obj, Printer out) {
		if (obj.isInfinite() || obj.isNaN()) {
			out.raw("null");
			return;
		}
		out.raw(Double.toString(obj));
	}
}
