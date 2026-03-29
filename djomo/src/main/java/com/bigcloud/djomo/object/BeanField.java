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
package com.bigcloud.djomo.object;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.TemporalType;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.error.AnnotationException;
import com.bigcloud.djomo.error.GetFieldException;
import com.bigcloud.djomo.error.SetFieldException;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.json.SafeString;
import com.bigcloud.djomo.poly.ResolverModel;

/**
 * General and primitive specialist bean field implementations
 * 
 * @author Alex Vigdor
 *
 */
public class BeanField implements Field, Cloneable {
	protected final MethodHandle accessor;
	protected final MethodHandle mutator;
	protected final String name;
	protected final Object key;
	protected final Model model;
	protected final Annotation[] annotations;

	public BeanField(MethodHandle accessor, MethodHandle mutator, String name, Model model, Annotation... annotations) {
		this.accessor = accessor;
		this.mutator = mutator == null
				? MethodHandles.empty(MethodType.methodType(void.class, Object.class, model.getType()))
				: mutator;
		this.name = name;
		if (name.length() < 1000) {
			this.key = new SafeString(name);
		} else {
			this.key = name;
		}
		this.model = model;
		this.annotations = annotations;
	}

	// Rekey constructor
	BeanField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
			Annotation... annotations) {
		this.accessor = accessor;
		this.mutator = mutator;
		this.name = name;
		if (key instanceof String cs && cs.length() < 1000) {
			key = new SafeString(cs);
		}
		this.key = key;
		this.model = model;
		this.annotations = annotations;
	}

	@Override
	public Object key() {
		return key;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		for (Annotation a : annotations) {
			if (annotationClass.isInstance(a)) {
				return (T) a;
			}
		}
		return null;
	}

	@Override
	public Model model() {
		return model;
	}

	@Override
	public Object get(Object o) {
		try {
			return accessor.invoke(o);
		} catch (Throwable e) {
			throw createGetException(o, e);
		}
	}

	protected GetFieldException createGetException(Object o, Throwable e) {
		return new GetFieldException("Error accessing " + name + " for " + o + " (" + o.getClass().getName() + ")",
				e);
	}

	@Override
	public void set(Object receiver, Object value) {
		try {
			mutator.invoke(receiver, value);
		} catch (Throwable e) {
			throw createSetException(receiver, value, e);
		}
	}

	protected SetFieldException createSetException(Object receiver, Object value, Throwable e) {
		return new SetFieldException("Error setting " + name + " = " + value + " for " + receiver + " ("
				+ receiver.getClass().getName() + ")", e);
	}

	@Override
	public void visit(Object source, Visitor visitor) {
		Object val;
		try {
			val = accessor.invoke(source);
		} catch (Throwable e) {
			throw createGetException(source, e);
		}
		visitor.visitObjectField(key);
		model.tryVisit(val, visitor);
	}

	@Override
	public void parse(Object dest, Parser parser) {
		var value = parser.parse(model);
		try {
			mutator.invoke(dest, value);
		} catch (Throwable e) {
			throw createSetException(dest, value, e);
		}
	}

	@Override
	public Field rekey(Object newKey) {
		return new BeanField(accessor, mutator, name, newKey, model, annotations);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class StringField extends BeanField {

		public StringField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
		}

		StringField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			CharSequence value = parser.parseString();
			try {
				if (value != null) {
					mutator.invoke(dest, value.toString());
				} else {
					mutator.invoke(dest, null);
				}
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			String val;
			try {
				val = (String) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			if (val == null) {
				visitor.visitNull();
			} else {
				visitor.visitString(val);
			}
		}

		@Override
		public Field rekey(Object newKey) {
			return new StringField(accessor, mutator, name, newKey, model, annotations);
		}

	}

	public static class TemporalField extends BeanField {
		TemporalType<?> temporalType;

		public TemporalField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
			temporalType = TemporalType.typeOf(model.getType());
		}

		TemporalField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
			temporalType = TemporalType.typeOf(model.getType());
		}

		@Override
		public void parse(Object dest, Parser parser) {
			TemporalAccessor value = parser.parseTemporal(temporalType);
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			TemporalAccessor val;
			try {
				val = (TemporalAccessor) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			if (val == null) {
				visitor.visitNull();
			} else {
				visitor.visitTemporal(val);
			}
		}

		@Override
		public Field rekey(Object newKey) {
			return new StringField(accessor, mutator, name, newKey, model, annotations);
		}

	}

	public static class DoubleField extends BeanField {

		public DoubleField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
		}

		DoubleField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			double value = parser.parseDouble();
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			double val;
			try {
				val = (double) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			visitor.visitDouble(val);
		}

		@Override
		public Field rekey(Object newKey) {
			return new DoubleField(accessor, mutator, name, newKey, model, annotations);
		}

	}

	public static class FloatField extends BeanField {

		public FloatField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
		}

		FloatField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			float value = parser.parseFloat();
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			float val;
			try {
				val = (float) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			visitor.visitFloat(val);
		}

		@Override
		public Field rekey(Object newKey) {
			return new FloatField(accessor, mutator, name, newKey, model, annotations);
		}
	}

	public static class LongField extends BeanField {

		public LongField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
		}

		public LongField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			long value = parser.parseLong();
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			long val;
			try {
				val = (long) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			visitor.visitLong(val);
		}

		@Override
		public Field rekey(Object newKey) {
			return new LongField(accessor, mutator, name, newKey, model, annotations);
		}
	}

	public static class IntField extends BeanField {

		public IntField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
		}

		IntField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			int value = parser.parseInt();
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			int val;
			try {
				val = (int) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			visitor.visitInt(val);
		}

		@Override
		public Field rekey(Object newKey) {
			return new IntField(accessor, mutator, name, newKey, model, annotations);
		}
	}

	public static class BooleanField extends BeanField {

		public BooleanField(MethodHandle accessor, MethodHandle mutator, String name, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
		}

		BooleanField(MethodHandle accessor, MethodHandle mutator, String name, Object key, Model model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			boolean value = parser.parseBoolean();
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			boolean val;
			try {
				val = (boolean) accessor.invoke(source);
			} catch (Throwable e) {
				throw createGetException(source, e);
			}
			visitor.visitObjectField(key);
			visitor.visitBoolean(val);
		}

		@Override
		public Field rekey(Object newKey) {
			return new BooleanField(accessor, mutator, name, newKey, model, annotations);
		}
	}

	public static class ObjectField extends BeanField {
		protected ObjectModel<?> objectModel;

		public ObjectField(MethodHandle accessor, MethodHandle mutator, String name, ObjectModel<?> model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
			this.objectModel = model;
		}

		ObjectField(MethodHandle accessor, MethodHandle mutator, String name, Object key, ObjectModel<?> model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
			this.objectModel = model;
		}

		@Override
		public void parse(Object dest, Parser parser) {
			var value = parser.parseObject(objectModel);
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public Field rekey(Object newKey) {
			return new ObjectField(accessor, mutator, name, newKey, objectModel, annotations);
		}
	}

	public static class ListField extends BeanField {
		protected ListModel<?> listModel;

		public ListField(MethodHandle accessor, MethodHandle mutator, String name, ListModel<?> model,
				Annotation... annotations) {
			super(accessor, mutator, name, model, annotations);
			this.listModel = model;
		}

		ListField(MethodHandle accessor, MethodHandle mutator, String name, Object key, ListModel<?> model,
				Annotation... annotations) {
			super(accessor, mutator, name, key, model, annotations);
			this.listModel = model;
		}

		@Override
		public void parse(Object dest, Parser parser) {
			var value = parser.parseList(listModel);
			try {
				mutator.invoke(dest, value);
			} catch (Throwable e) {
				throw createSetException(dest, value, e);
			}
		}

		@Override
		public Field rekey(Object newKey) {
			return new ListField(accessor, mutator, name, newKey, listModel, annotations);
		}
	}

	public static class Builder {
		private MethodHandle accessor;
		private MethodHandle mutator;
		private String name;
		private List<Annotation> annotations = new ArrayList<Annotation>();
		private Model model;

		public Field build() {
			Field field;
			var annotationArray = annotations.toArray(Annotation[]::new);
			if (model.getType() == String.class) {
				field = new StringField(accessor, mutator, name, model, annotationArray);
			} else if (model.getType() == double.class) {
				field = new DoubleField(accessor, mutator, name, model, annotationArray);
			} else if (model.getType() == float.class) {
				field = new FloatField(accessor, mutator, name, model, annotationArray);
			} else if (model.getType() == int.class) {
				field = new IntField(accessor, mutator, name, model, annotationArray);
			} else if (model.getType() == long.class) {
				field = new LongField(accessor, mutator, name, model, annotationArray);
			} else if (model.getType() == boolean.class) {
				field = new BooleanField(accessor, mutator, name, model, annotationArray);
			} else if(TemporalAccessor.class.isAssignableFrom(model.getType())) {
				field = new TemporalField(accessor, mutator, name, model, annotationArray);
			} else {
				if (model instanceof ResolverModel rm && rm.getResolver() instanceof Resolver.Substitute rs) {
					model = rs.getSubstitute();
				}
				if (model instanceof ObjectModel om) {
					field = new ObjectField(accessor, mutator, name, om, annotationArray);
				} else if (model instanceof ListModel lm) {
					field = new ListField(accessor, mutator, name, lm, annotationArray);
				} else {
					field = new BeanField(accessor, mutator, name, model, annotationArray);
				}
			}
			if (accessor == null) {
				field = new FilterField(field) {
					@Override
					public Object get(Object o) {
						return null;
					}

					@Override
					public void visit(Object source, Visitor visitor) {
					}
				};
			}
			return field;
		}

		public Builder mutator(MethodHandle mutator) {
			this.mutator = mutator;
			return this;
		}

		public Builder accessor(MethodHandle accessor) {
			this.accessor = accessor;
			return this;
		}

		public Builder model(Model model) {
			this.model = model;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder annotation(Annotation annotation) {
			for (Annotation a : annotations) {
				if (a.getClass().equals(annotation.getClass()) && !a.equals(annotation)) {
					throw new AnnotationException("Field " + name + " has two non-matching Annotations declared: "
							+ a + " and " + annotations);
				}
			}
			annotations.add(annotation);
			return this;
		}
	}

}
