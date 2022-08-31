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

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.io.Buffer;

public class BigDecimalModel extends NumberModel<BigDecimal> {

	public BigDecimalModel(Type type, ModelContext context) {
		super(type, context);
	}
	@Override
	public BigDecimal parse(String str) {
		return new BigDecimal(str);
	}

	@Override
	protected BigDecimal convertNumber(Number n) {
		return new BigDecimal(n.toString());
	}
	
	@Override
	public BigDecimal parse(Buffer readBuffer, Buffer overflow) throws IOException {
		overflow.writePosition = 0;
		int c;
		LOOP:
		while(true) {
			c = readBuffer.read();
			switch(c) {
				case -1:
					throw new ModelException("Unexpected EOF "+readBuffer.describe());
				case 43:
				case 45:
				case 46:
				case 48:
				case 49:
				case 50:
				case 51:
				case 52:
				case 53:
				case 54:
				case 55:
				case 56:
				case 57:
				case 69:
				case 101:
					overflow.append((char) (c));
					continue;
				case 34:
					break LOOP;
				default:
					readBuffer.unread();
					break LOOP;
			}
		}
		return new BigDecimal(overflow.buffer, 0, overflow.writePosition);
	}
}
