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
package com.bigcloud.djomo.base;

import java.lang.reflect.Type;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.api.Visitor;

public abstract class BaseSimpleModel<T> extends BaseModel<T> implements SimpleModel<T> {

	public BaseSimpleModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitSimple(obj, this);
	}

	protected String getParseable(Object in) {
		if (in == null) {
			return null;
		}
		String inStr = in.toString();
		if (inStr.length() == 0 || "null".equals(inStr)) {
			return null;
		}
		return inStr;
	}

}
