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

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;

public abstract class BaseModel<T> implements Model<T> {
	final protected Class<T> type;
	final protected Models models;

	public BaseModel(Type type, ModelContext context) {
		this(type, context.models());
		// we register this aggressively before any subclass constructor logic that
		// might lead to circular referencing
		context.set(type, this);
	}

	@SuppressWarnings("unchecked")
	public BaseModel(Type type, Models models) {
		this.models = models;
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			this.type = (Class<T>) pt.getRawType();
		} else {
			this.type = (Class<T>) type;
		}
	}

	@Override
	public T convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (T) o;
		}
		InstanceParser mp = new InstanceParser(models, o);
		return mp.filter(this);
	}

	@Override
	public void tryVisit(T obj, Visitor visitor) {
		if(obj == null) {
			visitor.visitNull();
		}
		else {
			visitor.visit(obj, this);
		}
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

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public String toString() {
		return getClass().getName() + "::" + getType().getName();
	}

	public Models models() {
		return models;
	}
}
