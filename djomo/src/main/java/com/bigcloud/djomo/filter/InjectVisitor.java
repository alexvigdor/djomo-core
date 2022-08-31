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
package com.bigcloud.djomo.filter;

import java.util.function.Function;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Inject a new, computed field value into a model dynamically during visitation.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to add a virtual field to
 */
public abstract class InjectVisitor<T> extends FilterVisitor {
	final Class<T> type;
	final String injectName;

	private InjectVisitor(Class<T> type, String injectName) {
		this.type = type;
		this.injectName = injectName;
	}

	public InjectVisitor(String injectName) {
		type = ConcreteType.get(this.getClass(), 0);
		this.injectName = injectName;
	}

	public abstract Object value(T obj);

	Object context;
	boolean inject;

	@Override
	public void visitObjectField(Object name, Object value) {
		if (inject) {
			inject = false;
			visitor.visitObjectField(injectName, value((T) context));
		}
		visitor.visitObjectField(name, value);
	}

	@Override
	public <O> void visitObject(O model, ObjectModel<O, ?, ?, ?, ?> definition) {
		var oc = context;
		context = model;
		inject = type.isInstance(model);
		visitor.visitObject(model, definition);
		inject = false;
		context = oc;
	}

	public static <T> InjectVisitor<T> inject(Class<T> type, String name, Function<T, Object> generator) {
		return new InjectVisitor<T>(type, name) {
			@Override
			public Object value(T obj) {
				return generator.apply(obj);
			}
		};
	}
}
