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

import java.nio.CharBuffer;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class CharArrayModel extends BaseModel<char[]> {

	public CharArrayModel(ModelContext context) {
		super(char[].class, context);
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

	@Override
	public char[] parse(Parser parser) {
		CharSequence cs = parser.parseString();
		int len = cs.length();
		char[] rval = new char[len];
		for(int i=0; i< len; i++) {
			rval[i] = cs.charAt(i);
		}
		return rval;
	}

	@Override
	public void visit(char[] obj, Visitor visitor) {
		visitor.visitString(CharBuffer.wrap(obj));
	}

}
