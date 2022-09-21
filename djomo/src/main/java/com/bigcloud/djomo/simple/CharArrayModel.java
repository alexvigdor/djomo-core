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

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.internal.CharSequenceParser;
import com.bigcloud.djomo.io.Buffer;

public class CharArrayModel extends BaseSimpleModel<char[]> {

	public CharArrayModel(ModelContext context) {
		super(char[].class, context);
	}

	@Override
	public void print(char[] obj, Printer printer) {
		printer.quote(new String(obj));
	}

	@Override
	public char[] parse(Buffer input, Buffer overflow) throws IOException {
		CharSequence cs = CharSequenceParser.parse(input, overflow);
		int len = cs.length();
		char[] rval = new char[len];
		for(int i=0; i< len; i++) {
			rval[i] = cs.charAt(i);
		}
		return rval;
	}

	@Override
	public char[] convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (char[]) o;
		}
		return o.toString().toCharArray();
	}

}
