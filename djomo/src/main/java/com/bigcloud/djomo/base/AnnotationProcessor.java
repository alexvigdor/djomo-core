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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Parses;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.annotation.Visits;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.api.ParserTypedFilterFactory;
import com.bigcloud.djomo.api.VisitorFilterFactory;
import com.bigcloud.djomo.api.VisitorTypedFilterFactory;
import com.bigcloud.djomo.error.AnnotationException;
import com.bigcloud.djomo.filter.parsers.PathParser;
import com.bigcloud.djomo.filter.visitors.PathVisitor;

/**
 * Responsible for converting Visit and Parse annotations into FilterVisitors
 * and FilterParsers.
 * 
 * Capable of performing some basic constructor injection for filters, including
 * models, types and string args
 * 
 * @author Alex Vigdor
 *
 */
public class AnnotationProcessor {
	private final ConcurrentHashMap<Visit, VisitorFilterFactory> visitors = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Parse, ParserFilterFactory> parsers = new ConcurrentHashMap<>();
	private final Models models;
	private final Object[] dependencies;

	public AnnotationProcessor(Models models, Object... dependencies) {
		this.models = models;
		this.dependencies = dependencies.clone();
	}

	public VisitorFilterFactory[] visitorFilters(Class<?> type) {
		return visitorFilters(filters(type, Visit.class));
	}

	public VisitorFilterFactory[] visitorFilters(Annotation[] annotations) {
		PathVisitor.Builder visitorPathBuilder = PathVisitor.builder();
		ArrayDeque<VisitorFilterFactory> visitorFilters = new ArrayDeque<>();
		for (var a : annotations) {
			if (a instanceof Visit ser) {
				configure(ser, visitorFilters, visitorPathBuilder);
			} else if (a instanceof Visits sers) {
				for (var ser : sers.value()) {
					configure(ser, visitorFilters, visitorPathBuilder);
				}
			}
		}
		if (!visitorPathBuilder.isEmpty()) {
			visitorFilters.add(visitorPathBuilder.build());
		}
		return visitorFilters.toArray(new VisitorFilterFactory[0]);
	}

	public ParserFilterFactory[] parserFilters(Class<?> type) {
		return parserFilters(filters(type, Parse.class));
	}

	public ParserFilterFactory[] parserFilters(Annotation[] annotations) {
		PathParser.Builder parserPathBuilder = PathParser.builder();
		ArrayDeque<ParserFilterFactory> parserFilters = new ArrayDeque<>();
		for (var a : annotations) {
			if (a instanceof Parse deser) {
				configure(deser, parserFilters, parserPathBuilder);
			} else if (a instanceof Parses desers) {
				for (var deser : desers.value()) {
					configure(deser, parserFilters, parserPathBuilder);
				}
			}
		}
		if (!parserPathBuilder.isEmpty()) {
			parserFilters.add(parserPathBuilder.build());
		}
		return parserFilters.toArray(new ParserFilterFactory[0]);
	}

	private void missingConstructor(Class<?> filterClass) throws InstantiationException {
		StringBuilder messageBuilder = new StringBuilder("No matching constructor for ").append(filterClass.getName())
				.append(" ; try adding args or injecting dependencies to mmatch a constructor [");
		for (Constructor c : filterClass.getConstructors()) {
			messageBuilder.append("\n\t").append(filterClass.getSimpleName()).append("(");
			boolean first = true;
			for (Parameter p : c.getParameters()) {
				if (first) {
					first = false;
				} else {
					messageBuilder.append(", ");
				}
				messageBuilder.append(p.getParameterizedType().getTypeName()).append(" ").append(p.getName());
			}
			messageBuilder.append(")");
		}
		messageBuilder.append("\n]");
		throw new InstantiationException(messageBuilder.toString());
	}

	private void configure(Parse deser, ArrayDeque<ParserFilterFactory> filters, PathParser.Builder pathBuilder) {
		try {
			ParserFilterFactory filterParser = parsers.get(deser);
			if (filterParser == null) {
				var filterClass = deser.value();
				var filterType = deser.type();
				filterParser = (ParserFilterFactory) typedFilter(filterClass, filterType, deser.arg());
				if (filterParser == null) {
					filterParser = (ParserFilterFactory) typedFilter(filterClass, null, deser.arg());
					if (filterParser == null) {
						missingConstructor(filterClass);
					}
					if (filterType != Object.class) {
						throw new InstantiationException("Filter does not support type "+filterClass.getTypeName());
					}
				}
				parsers.put(deser, filterParser);
			}
			if (deser.path().length > 0) {
				for (String path : deser.path()) {
					pathBuilder.filter(path, filterParser);
				}

			} else if (!pathBuilder.isEmpty()) {
				// add it as path with wildcard to maintain order
				pathBuilder.filter("**", filterParser);
			} else {
				filters.add(filterParser);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| SecurityException e) {
			throw new AnnotationException("Unable to make FilterParser from annotation " + deser, e);
		}
	}

	private Object typedFilter(Class filterClass, Class filterType, String[] args)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object filter = null;
		CONSTRUCTOR_SEARCH: for (Constructor<?> constructor : filterClass.getConstructors()) {
			// we'll do our best to construct with matching parameters, including Models,
			// filterType, and string args
			Parameter[] parms = constructor.getParameters();
			boolean foundType = false;
			Object[] values = new Object[parms.length];
			int argPointer = 0;
			PARM_LOOP: for (int i = 0; i < parms.length; i++) {
				Parameter parm = parms[i];
				Class parmType = parm.getType();
				if (parmType == Class.class) {
					if (filterType == null || foundType) {
						continue CONSTRUCTOR_SEARCH;
					}
					foundType = true;
					values[i] = filterType;
				} else if (Model.class.isAssignableFrom(parmType)) {
					if (filterType == null || foundType) {
						continue CONSTRUCTOR_SEARCH;
					}
					foundType = true;
					values[i] = models.get(filterType);
				} else if (parmType == Models.class) {
					values[i] = models;
				} else if (parmType == String.class) {
					if (argPointer == args.length) {
						continue CONSTRUCTOR_SEARCH;
					}
					values[i] = args[argPointer++];
				} else if (parmType == String[].class) {
					if (argPointer == args.length && !parm.isVarArgs()) {
						continue CONSTRUCTOR_SEARCH;
					}
					String[] val = args;
					if (argPointer > 0) {
						val = Arrays.copyOfRange(val, argPointer, args.length);
					}
					values[i] = val;
					argPointer = args.length;
				} else {
					for (Object d : dependencies) {
						if (parmType.isInstance(d)) {
							values[i] = d;
							continue PARM_LOOP;
						}
					}
					continue CONSTRUCTOR_SEARCH;
				}
			}
			if (filterType == null || foundType) {
				filter = constructor.newInstance(values);
				break;
			} else if (filterType != null) {
				if (filterClass.isAssignableFrom(VisitorTypedFilterFactory.class)) {
					filter = ((VisitorTypedFilterFactory) constructor.newInstance(values)).newVisitorFilter(filterType);
					break;
				} else if (filterClass.isAssignableFrom(ParserTypedFilterFactory.class)) {
					filter = ((ParserTypedFilterFactory) constructor.newInstance(values)).newParserFilter(filterType);
					break;
				}
			}

		}
		return filter;
	}

	private void configure(Visit ser, ArrayDeque<VisitorFilterFactory> filters, PathVisitor.Builder pathBuilder) {
		try {
			VisitorFilterFactory filterVisitor = visitors.get(ser);
			if (filterVisitor == null) {
				var filterClass = ser.value();
				var filterType = ser.type();
				filterVisitor = (VisitorFilterFactory) typedFilter(filterClass, filterType, ser.arg());
				if (filterVisitor == null) {
					filterVisitor = (VisitorFilterFactory) typedFilter(filterClass, null, ser.arg());
					if (filterVisitor == null) {
						missingConstructor(filterClass);
					}
					if (filterType != Object.class) {
						throw new InstantiationException("Filter does not support type "+filterClass.getTypeName());
					}
				}
				visitors.put(ser, filterVisitor);
			}
			if (ser.path().length > 0) {
				for (String path : ser.path()) {
					pathBuilder.filter(path, filterVisitor);
				}
			} else if (!pathBuilder.isEmpty()) {
				// add it as path with wildcard to maintain order
				pathBuilder.filter("**", filterVisitor);
			} else {
				filters.add(filterVisitor);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| SecurityException e) {
			throw new AnnotationException("Unable to make FilterVisitor from annotation " + ser, e);
		}
	}

	private <F extends Annotation> F[] filters(Class<?> type, Class<F> filterType) {
		Deque<F> filters = new ArrayDeque<>();
		Set<Class<?>> visited = new HashSet<>();
		Deque<Class<?>> queue = new ArrayDeque<>();
		visited.add(type);
		queue.add(type);
		while (!queue.isEmpty()) {
			Class<?> c = queue.pop();
			for (F vf : c.getDeclaredAnnotationsByType(filterType)) {
				filters.add(vf);
			}
			for (Class<?> i : c.getInterfaces()) {
				if (visited.add(i)) {
					queue.add(i);
				}
			}
			c = c.getSuperclass();
			if (c != null && visited.add(c)) {
				queue.add(c);
			}
		}
		return (F[]) filters.toArray(new Annotation[0]);
	}
}
