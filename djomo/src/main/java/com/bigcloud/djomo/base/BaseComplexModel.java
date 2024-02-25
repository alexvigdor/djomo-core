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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ModelContext;

public abstract class BaseComplexModel<T> extends BaseModel<T> {
	final protected Map<String, Type> typeArgs;

	public BaseComplexModel(Type type, ModelContext context) {
		super(type, context);
		typeArgs = getTypeArgs(type);
	}

	public BaseComplexModel(Type type, Models models) {
		super(type, models);
		typeArgs = getTypeArgs(type);
	}

	private Map<String, Type> getTypeArgs(Type type) {
		Map<String, Type> typeArgs = null;
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			var args = pt.getActualTypeArguments();
			var parms = ((Class) pt.getRawType()).getTypeParameters();
			typeArgs = new LinkedHashMap<>();
			for (int i = 0; i < args.length; i++) {
				typeArgs.put(parms[i].getName(), args[i]);
			}
			return typeArgs;
		}
		return typeArgs;
	}

	public String toString() {
		String sup = super.toString();
		if (typeArgs == null) {
			return sup;
		}
		return sup + " " + typeArgs;
	}
}
