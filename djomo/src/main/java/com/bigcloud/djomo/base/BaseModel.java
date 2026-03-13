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
import java.util.HashMap;
import java.util.Map;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.annotation.Property;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.error.AnnotationException;

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
		return (T) mp.parse(this);
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

	protected Map<CharSequence, Field> expandFieldMap(Field[] fields){
		Map<CharSequence, Field> expandedFields = new HashMap<>();
		// first populate fields into the map and check for conflicts
		for(Field f: fields) {
			String key = f.key().toString();
			if(expandedFields.containsKey(key)) {
				throw new AnnotationException("More than one field defined with property name '"+key+"' for type "+type.getTypeName());
			}
			expandedFields.put(f.key().toString(), f);
		}
		// next, populate aliases into the map, checking for conflicts
		for(Field f: fields) {
			Property prop = f.getAnnotation(Property.class);
			if(prop != null) {
				for(String alias: prop.alias()) {
					if(expandedFields.containsKey(alias)) {
						throw new AnnotationException("Alias '"+alias+"' for field '"+f.key()+"' conflicts with field '"+expandedFields.get(alias).key()+"' for type "+type.getTypeName());
					}
					expandedFields.put(alias,  f);
				}
			}
		}
		return expandedFields;
	}
}
