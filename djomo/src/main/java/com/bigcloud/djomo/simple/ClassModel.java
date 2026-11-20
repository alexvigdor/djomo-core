/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ModelFactory;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.internal.CharSequenceLookup;

public class ClassModel extends BaseModel<Class<?>> {
	final CharSequenceLookup<Class<?>> lookup;

	public ClassModel(Type type, ModelContext context, Class<?>... allowedClasses) {
		super(type, context);
		Map<CharSequence, Class<?>> map = new HashMap<>(allowedClasses.length);
		for (Class<?> candidate : allowedClasses) {
			if (candidate != null && isTypeAllowed(type, candidate)) {
				map.put(candidate.getName(), candidate);
			}
		}
		lookup = new CharSequenceLookup<Class<?>>(map);
	}

	@Override
	public Class<?> parse(Parser parser) {
		var className = parser.parseString();
		if (className == null) {
			return null;
		}
		Class<?> clazz = lookup.get(className);
		if (clazz == null) {
			throw new IllegalArgumentException("Class " + className + " is not in the parser allowlist.");
		}
		return clazz;
	}

	@Override
	public void visit(Class<?> obj, Visitor visitor) {
		visitor.visitString(obj.getName());
	}

	public static ModelFactory allowing(Class<?>... allowedClasses) {
		return (type, context) -> new ClassModel(type, context, allowedClasses);
	}
	
	private boolean isTypeAllowed(Type type, Class<?> candidate) {
		if (type instanceof ParameterizedType pt && pt.getRawType() == Class.class) {
			return satisfiesTypeArg(candidate, pt.getActualTypeArguments()[0]);
		}
		return true;
	}

	private static boolean satisfiesTypeArg(Class<?> candidate, Type typeArg) {
		if (typeArg instanceof WildcardType wt) {
			Type[] lowerBounds = wt.getLowerBounds();
			if (lowerBounds.length > 0) {
				return candidate.isAssignableFrom(eraseToClass(lowerBounds[0]));
			}
			return eraseToClass(wt.getUpperBounds()[0]).isAssignableFrom(candidate);
		}
		if (typeArg instanceof TypeVariable<?> tv) {
			for (Type bound : tv.getBounds()) {
				if (!eraseToClass(bound).isAssignableFrom(candidate)) {
					return false;
				}
			}
			return true;
		}
		return candidate == eraseToClass(typeArg);
	}

	private static Class<?> eraseToClass(Type t) {
		if (t instanceof Class<?> c) {
			return c;
		}
		if (t instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw) {
			return raw;
		}
		if (t instanceof GenericArrayType gat) {
			return eraseToClass(gat.getGenericComponentType()).arrayType();
		}
		if (t instanceof TypeVariable<?> tv) {
			return eraseToClass(tv.getBounds()[0]);
		}
		return Object.class;
	}
}
