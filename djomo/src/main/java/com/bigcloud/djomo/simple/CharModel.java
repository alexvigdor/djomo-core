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
import com.bigcloud.djomo.internal.CharSequenceParser;
import com.bigcloud.djomo.io.Buffer;

public class CharModel extends BaseSimpleModel<Character>{

	public CharModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public void print(Character obj, Printer printer) {
		printer.raw(new char[] {'"', obj.charValue(), '"'}, 0, 3);
	}

	@Override
	public Character convert(Object o) {
		if(o==null) {
			return null;
		}
		if(o instanceof Character) {
			return (Character) o;
		}
		return getParseable(o).charAt(0);
	}

	@Override
	public Character parse(Buffer input, Buffer overflow) throws IOException {
		return CharSequenceParser.parse(input, overflow).charAt(0);
	}
}
