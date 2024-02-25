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
package com.bigcloud.djomo;

import java.lang.reflect.Type;

import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.internal.ConcreteType;

/**
 * Base class for resolvers; responsible for producing an instance of an
 * interface or abstract type from a parser. A resolver might have a specific
 * implementation in mind, or can parse to some basic form like a string or map
 * to analyze the data before converting to the desired concrete type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the class for which this resolver should intercept parse calls
 */
public abstract class Resolver<T> implements Cloneable {
	final Class<T> type;

	public Resolver() {
		type = ConcreteType.get(getClass(), 0);
	}

	public Resolver(Class<T> type) {
		this.type = type;
	}

	public Class<T> getType() {
		return type;
	}

	public void init(ModelContext models, Type[] typeArgs) {
	}

	public abstract T resolve(Parser parser);
	
	/**
	 * 
	 * @return the format this resolver parses from
	 */
	public abstract Format getFormat();

	public Resolver<T> clone() {
		Resolver<T> cloned;
		try {
			cloned = (Resolver<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return cloned;
	}
	/**
	 * <p>
	 * Use this resolver to always pick one specific concrete subtype of an interface for parsing
	 * </p>
	 * <p>
	 * {@code Models.builder().resolver(new Resolver.Substitute<Map, ConcurrentHashMap>() {}) ... }
	 * </p>
	 *
	 * @param <T> the interface or abstract type to replace
	 * @param <S> the concrete subtype to substitute when asked for T
	 */
	public static class Substitute<T, S extends T> extends Resolver<T> {
		Class<S> substituteClass;
		Model<S> substituteModel;

		public Substitute() {
			super();
			substituteClass = ConcreteType.get(getClass(), 1);
		}

		public Substitute(Class<T> type, Class<S> substitute) {
			super(type);
			this.substituteClass = substitute;
		}

		public Model<S> getSubstitute() {
			return substituteModel;
		}

		@Override
		public T resolve(Parser parser) {
			return substituteModel.parse(parser);
		}

		@Override
		public void init(ModelContext models, Type[] typeArgs) {
			if (typeArgs != null) {
				substituteModel = (Model<S>) models.get(ModelType.of(substituteClass, typeArgs));
			} else {
				substituteModel = models.get(substituteClass);
			}
		}

		@Override
		public Format getFormat() {
			return substituteModel.getFormat();
		}
	}
}
