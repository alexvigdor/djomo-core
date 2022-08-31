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
package com.bigcloud.djomo.poly;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.base.ModelParser;

public class ResolverModel<T> extends BaseModel<T> {
	private final Resolver<T> resolver;
	private final ModelContext context;
	private final Type[] typeArgs;

	public ResolverModel(Type type, ModelContext context, Resolver<T> resolver) {
		super(type, context);
		if (!this.type.isInterface() && !Modifier.isAbstract(this.type.getModifiers())) {
			throw new IllegalArgumentException(
					"Resolvers may only be used with interfaces or abstract classes; for other classes like "
							+ this.type.getTypeName() + " use a ParserFilter");
		}
		if(type instanceof ParameterizedType) {
			typeArgs =  ((ParameterizedType) type).getActualTypeArguments();
		}
		else {
			typeArgs = null;
		}
		this.context = context;
		this.resolver = resolver.clone();
		this.resolver.init(context, typeArgs);
	}

	@Override
	public T convert(Object o) {
		return new ModelParser(context.models(), o).filter(this);
	}

	@Override
	public T parse(Parser parser) {
		return resolver.resolve(parser);
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		throw new UnsupportedOperationException();
	}

	public Resolver<T> getResolver() {
		return resolver;
	}
}
