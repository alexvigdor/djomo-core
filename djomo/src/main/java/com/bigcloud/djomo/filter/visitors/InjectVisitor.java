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
package com.bigcloud.djomo.filter.visitors;

import java.util.function.Function;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
import com.bigcloud.djomo.internal.ConcreteType;

/**
 * Inject a new, computed field value into a model dynamically during
 * visitation.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to add a virtual field to
 */
public abstract class InjectVisitor<T> extends BaseVisitorFilter {
	final FilterFieldObjectModels injectModels;
	final Class<T> type;
	final String injectName;

	private InjectVisitor(Class<T> type, String injectName) {
		this.type = type;
		this.injectName = injectName;
		this.injectModels = new FilterFieldObjectModels(this::init);
	}

	public InjectVisitor(String injectName) {
		type = ConcreteType.get(this.getClass(), 0);
		this.injectName = injectName;
		this.injectModels = new FilterFieldObjectModels(this::init);
	}

	private Stream<Field> init(Stream<Field> fields) {
		return Stream.concat(Stream.of(new FilterField(null) {
			@Override
			public Object key() {
				return injectName;
			}

			@Override
			public void visit(Object source, Visitor visitor) {
				visitor.visitObjectField(injectName);
				visitor.visit(value((T) source));
			}

		}), fields);
	}

	public abstract Object value(T obj);

	@Override
	public <O> void visitObject(O obj, ObjectModel<O> model) {
		if (type.isInstance(obj)) {
			visitor.visitObject(obj, injectModels.getFilteredObjectModel(model));
		} else {
			visitor.visitObject(obj, model);
		}
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
