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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bigcloud.djomo.Resolver.Substitute;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ModelFactory;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.base.BaseModelFactory;
import com.bigcloud.djomo.list.ListModelFactory;
import com.bigcloud.djomo.object.ObjectModelFactory;
import com.bigcloud.djomo.poly.AnyModel;
import com.bigcloud.djomo.poly.DefaultResolverModelFactory;
import com.bigcloud.djomo.poly.PolyModelFactory;
import com.bigcloud.djomo.poly.ResolverModel;
import com.bigcloud.djomo.poly.ResolverModelFactory;
import com.bigcloud.djomo.simple.NumberModel;
import com.bigcloud.djomo.simple.SimpleModelFactory;
/**
 * <p>
 * A pull-through cache for {@link Model} implementations looked up using java Types and Classes, loaded from a fallback chain of user-provided and default {@link ModelFactory}.
 * </p><p>
 * Models provide the core data-mapping logic used by {@link Json} for parsing and serializing.  Default factories support standard java primitives, 
 * collections, java time classes, as well as any classes following standard java beans or builder conventions.
 * </p><p>
 * While {@link ParserFilter} and {@link VisitorFilter} can be used to customize the high-level operation of a parser or serializer, there may be cases where
 * you want to provide a custom model implementation or define custom logic for how to create a concrete instance of an interface or abstract type.
 * To do so use a {@link Models.Builder}, to which you can pass both custom {@link ModelFactory} and {@link Resolver} implementations.
 * </p>
 * 
 * @author Alex Vigdor
 *
 */
public class Models {
	private final ConcurrentHashMap<Type, Model<?>> models = new ConcurrentHashMap<>();
	private final ModelFactory[] modelFactories;
	public final AnyModel anyModel;
	public final ObjectModel<Map<?,?>> mapModel;
	public final ListModel<List<?>> listModel;
	public final ListModel<Stream<?>> streamModel;
	public final NumberModel<Number> numberModel;

	public Models(ModelFactory... factories) {
		if(factories == null || factories.length == 0) {
			modelFactories = new ModelFactory[5];
		}
		else {
			modelFactories = new ModelFactory[factories.length+5];
			System.arraycopy(factories, 0, modelFactories, 0, factories.length);
		}
		modelFactories[modelFactories.length-5] = new DefaultResolverModelFactory();
		modelFactories[modelFactories.length-4] = new SimpleModelFactory();
		modelFactories[modelFactories.length-3] = new ListModelFactory();
		modelFactories[modelFactories.length-2] = new PolyModelFactory();
		modelFactories[modelFactories.length-1] = new ObjectModelFactory();
		anyModel = get(Object.class);
		streamModel = (ListModel) get(Stream.class);
		Model tm = get(Map.class);
		ObjectModel<Map<?,?>> mapModel = null;
		if(tm instanceof ResolverModel) {
			Resolver r = ((ResolverModel)tm).getResolver();
			if(r instanceof Substitute) {
				mapModel = (ObjectModel<Map<?,?>>) ((Substitute) r).getSubstitute();
			}
		}
		if(mapModel == null) {
			mapModel = (ObjectModel) get(LinkedHashMap.class);
		}
		this.mapModel = mapModel;
		Model tl = get(List.class);
		ListModel<List<?>> listModel = null;
		if(tl instanceof ResolverModel) {
			Resolver r = ((ResolverModel)tl).getResolver();
			if(r instanceof Substitute) {
				listModel = (ListModel<List<?>>) ((Substitute) r).getSubstitute();
			}
		}
		else if(tl instanceof ListModel){
			listModel = (ListModel) tl;
		}
		this.listModel = listModel;
		this.numberModel = get(Number.class);
	}
	
	public static Builder builder() {
		return new Builder();
	}

	public <T, M extends Model<T>> M get(Class<T> clazz) {
		return get((Type) clazz);
	}

	public <M extends Model<?>> M get(Type type) {
		Model<?> model = models.get(type);
		if (model == null) {
			TempContext tc = new TempContext();
			model = create(type, tc);
			tc.release();
		}
		return (M) model;
	}

	private Model<?> create(Type type, TempContext tc) {
		Model<?> model;
		var resolvedType = resolve(type);
		// try service loader factories
		for (var factory : modelFactories) {
			model = factory.create(resolvedType, tc);
			if (model != null) {
				return model;
			}
		}
		throw new IllegalArgumentException("Unable to locate model for type "+type.getTypeName());
	}

	private Type resolve(Type type) {
		if (type instanceof Class) {
			return type;
		}
		if (type instanceof ParameterizedType) {
			return type;
		}
		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			return resolve(tv.getBounds()[0]);
		}
		if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			if (wt.getLowerBounds().length > 0) {
				return resolve(wt.getLowerBounds()[0]);
			} else {
				return resolve(wt.getUpperBounds()[0]);
			}
		}
		if (type instanceof GenericArrayType) {
			GenericArrayType gt = (GenericArrayType) type;
			Type t = resolve(gt.getGenericComponentType());
			if (t instanceof ParameterizedType) {
				return ((Class) ((ParameterizedType) t).getRawType()).arrayType();
			}
			return ((Class) t).arrayType();
		}
		throw new IllegalArgumentException(type.getTypeName() + " " + type.getClass().getName());
	}

	private class TempContext implements ModelContext {
		Map<Type, Model<?>> tempModels = new HashMap<>();

		@Override
		public Model<?> get(Type type) {
			Model<?> model = tempModels.get(type);
			if (model != null) {
				return model;
			}
			model = models.get(type);
			if (model != null) {
				return model;
			}
			return Models.this.create(type, this);
		}

		@Override
		public void set(Type type, Model<?> model) {
			tempModels.put(type, model);
		}

		public void release() {
			tempModels.forEach(models::putIfAbsent);
		}

		public Models models() {
			return Models.this;
		}
	}

	/**
	 * Builder to customize a Models with ModelFactories and Resolvers
	 */
	public static class Builder {
		ArrayDeque<Resolver<?>> resolvers = new ArrayDeque<>();
		ArrayDeque<ModelFactory> factories = new ArrayDeque<>();

		/**
		 * 
		 * @param resolvers one or more resolvers to apply to the Models
		 * @return this builder
		 */
		public Builder resolver(Resolver<?>... resolvers) {
			for (Resolver<?> resolver : resolvers) {
				this.resolvers.add(resolver);
			}
			return this;
		}

		/**
		 * Use a java service loader to discover and load ModelFactory instances
		 * 
		 * @return this builder
		 */
		public Builder loadFactories() {
			ServiceLoader.load(ModelFactory.class).forEach(factories::add);
			return this;
		}

		/**
		 * Pass in one or more ModelFactory instances to supply Models
		 * 
		 * @return this builder
		 */
		public Builder factory(ModelFactory... factories) {
			for (ModelFactory factory : factories) {
				this.factories.add(factory);
			}
			return this;
		}

		/**
		 * Register an explicit model factory for a type
		 * 
		 * @param type
		 * @param factory
		 * @return
		 */
		public <T> Builder factory(Class<T> type, ModelFactory factory) {
			return factory(new BaseModelFactory() {

				@Override
				public Model<?> create(Type lookupType, ModelContext context) {
					Class rawType = getRawType(lookupType);
					if (type.isAssignableFrom(rawType)) {
						return factory.create(lookupType, context);
					}
					return null;
				}
			});
		}

		/**
		 * Register an explicit model based on type, visitor and parser
		 * functions
		 * 
		 * @param type
		 * @param visitor
		 * @param parser
		 * @return
		 */
		public <T> Builder model(Class<T> type, BiConsumer<T, Visitor> visitor,
				Function<Parser, T> parser) {
			return factory(type, (realType, context) -> new BaseModel<T>(realType, context) {

				@Override
				public T parse(Parser source) {
					return parser.apply(source);
				}

				@Override
				public void visit(T obj, Visitor dest) {
					visitor.accept(obj, dest);
				}
			});
		}

		public Models build() {
			ModelFactory[] useFactories;
			if (resolvers.isEmpty()) {
				useFactories = factories.toArray(new ModelFactory[factories.size()]);
			} else {
				useFactories = factories.toArray(new ModelFactory[factories.size() + 1]);
				useFactories[factories.size()] = new ResolverModelFactory(resolvers.toArray(new Resolver[0]));
			}
			Models models = new Models(useFactories);
			return models;
		}
	}
}
