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

public class ShortModel extends NumberModel<Short>{

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
	public void print(Short obj, Printer out) {
		int l = obj.shortValue();
		boolean negative = l < 0;
		if (negative) {
			l = 0 - l;
		}
		char[] buf = new char[6];
		int pos = 6;
		do {
			buf[--pos] = (char) (48 + (l % 10));
			l /= 10;
		} while (l > 0);
		if (negative) {
			buf[--pos] = '-';
		}
		out.raw(buf, pos, 6 - pos);
	}
}
