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

import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class StringModel extends BaseModel<String> {

	public StringModel(ModelContext context) {
		super(String.class, context);
	}
	
	@Override
	public String convert(Object o) {
		if(o==null) {
			return null;
		}
		return o.toString();
	}

	@Override
	public void visit(String obj, Visitor visitor) {
		visitor.visitString(obj);
	}

	@Override
	public String parse(Parser parser) {
		return parser.parseString().toString();
	}
	@Override
	public Format getFormat() {
		return Format.STRING;
	}
}
