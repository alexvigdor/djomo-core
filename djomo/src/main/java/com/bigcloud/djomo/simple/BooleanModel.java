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

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.error.UnexpectedPrimitiveException;
import com.bigcloud.djomo.io.Buffer;

public class BooleanModel extends BaseSimpleModel<Boolean>{

	public BooleanModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public void print(Boolean obj, Printer printer) {
		if(obj.booleanValue()) {
			printer.raw("true");
		}
		else {
			printer.raw("false");
		}
	}

	@Override
	public Boolean convert(Object o) {
		if(o==null) {
			return Boolean.FALSE;
		}
		if(o instanceof Boolean) {
			return (Boolean) o;
		}
		return Boolean.valueOf(getParseable(o));
	}

	@Override
	public Boolean parse(Buffer input, Buffer overflow) throws IOException {
		try {
			var b = input;
			var c = b.read();
			if(c == 't') {
					if((c = b.read()) == 'r' && (c = b.read()) == 'u' && (c = b.read()) == 'e'){
						return Boolean.TRUE;
					}
					throw new UnexpectedPrimitiveException("Unexepected character in true "+(char)c);
			}
			else if(c == 'f') {
				if((c = b.read()) == 'a' && (c = b.read()) == 'l' && (c = b.read()) == 's' && (c = b.read()) == 'e'){
					return Boolean.FALSE;
				}
				throw new UnexpectedPrimitiveException("Unexepected character in false "+(char)c);
			}
			throw new UnexpectedPrimitiveException("Unexepected character in boolean "+(char)c);
		}
		catch(IOException e) {
			throw new ModelException("Error parsing boolean", e);
		}
	}
	
}
