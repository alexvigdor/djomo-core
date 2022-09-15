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

public class LongModel extends NumberModel<Long> {

	public LongModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public Long parse(String str) {
		return Long.valueOf(str);
	}

	@Override
	public Long convertNumber(Number n) {
		if (n instanceof Long l) {
			return l;
		}
		return n.longValue();
	}

	@Override
	protected Long convertDouble(double value) {
		return (long) value;
	}

	@Override
	protected Long convertInt(int value) {
		return (long) value;
	}

	@Override
	protected Long convertLong(long value) {
		return value;
	}

	@Override
	public void print(Long obj, Printer out) {
		long l = obj.longValue();
		int mult = l < 0 ? -1 : 1;
		char[] buf = new char[20];
		int pos = 20;
		do {
			buf[--pos] = (char) (48 + ((l % 10) * mult));
			l /= 10;
		} while (l != 0);
		if (mult == -1) {
			buf[--pos] = '-';
		}
		out.raw(buf, pos, 20 - pos);
	}
}
