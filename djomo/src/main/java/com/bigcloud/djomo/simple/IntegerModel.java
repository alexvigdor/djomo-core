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

public class IntegerModel extends NumberModel<Integer>{

	public IntegerModel(Type type, ModelContext context) {
		super(type, context);
	}
	@Override
	public Integer parse(String str) {
		return Integer.valueOf(str);
	}

	@Override
	protected Integer convertNumber(Number n) {
		return n.intValue();
	}

	@Override
	public void print(Integer obj, Printer out) {
		int l = obj.intValue();
		int mult = l < 0 ? -1 : 1;
		char[] buf = new char[11];
		int pos = 11;
		do {
			buf[--pos] = (char) (48 + ((l % 10) * mult));
			l /= 10;
		} while (l != 0);
		if (mult == -1) {
			buf[--pos] = '-';
		}
		out.raw(buf, pos, 11 - pos);
	}
}
