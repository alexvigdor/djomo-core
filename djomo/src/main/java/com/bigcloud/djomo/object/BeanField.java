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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.error.GetFieldException;
import com.bigcloud.djomo.error.SetFieldException;
import com.bigcloud.djomo.filter.FilterField;
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
	protected Object key;
	protected final Model model;

	public BeanField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
		this.accessor = accessor;
		this.mutator = mutator == null
				? MethodHandles.empty(MethodType.methodType(void.class, Object.class, model.getType()))
				: mutator;
		this.name = name;
		this.key = name;
		this.model = model;
	}

	@Override
	public Object key() {
		return key;
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
		BeanField cloned = clone();
		cloned.key = newKey;
		return cloned;
	}

	protected BeanField clone() {
		try {
			return (BeanField) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class StringField extends BeanField {

		public StringField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
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

	}

	public static class DoubleField extends BeanField {

		public DoubleField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
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

	}

	public static class FloatField extends BeanField {

		public FloatField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
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
	}

	public static class LongField extends BeanField {

		public LongField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
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

	}

	public static class IntField extends BeanField {

		public IntField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
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
	}

	public static class BooleanField extends BeanField {

		public BooleanField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
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
	}

	public static class ObjectField extends BeanField {
		protected ObjectModel<?> objectModel;

		public ObjectField(MethodHandle accessor, MethodHandle mutator, String name, ObjectModel<?> model) {
			super(accessor, mutator, name, model);
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

	}

	public static class ListField extends BeanField {
		protected ListModel<?> listModel;

		public ListField(MethodHandle accessor, MethodHandle mutator, String name, ListModel<?> model) {
			super(accessor, mutator, name, model);
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

	}

	public static class Builder {
		private MethodHandle accessor;
		private MethodHandle mutator;
		private String name;
		private Model model;

		public Field build() {
			Field field;
			if (model.getType() == String.class) {
				field = new StringField(accessor, mutator, name, model);
			} else if (model.getType() == double.class) {
				field = new DoubleField(accessor, mutator, name, model);
			} else if (model.getType() == float.class) {
				field = new FloatField(accessor, mutator, name, model);
			} else if (model.getType() == int.class) {
				field = new IntField(accessor, mutator, name, model);
			} else if (model.getType() == long.class) {
				field = new LongField(accessor, mutator, name, model);
			} else if (model.getType() == boolean.class) {
				field = new BooleanField(accessor, mutator, name, model);
			} else {
				if (model instanceof ResolverModel rm && rm.getResolver() instanceof Resolver.Substitute rs) {
					model = rs.getSubstitute();
				}
				if (model instanceof ObjectModel om) {
					field = new ObjectField(accessor, mutator, name, om);
				} else if (model instanceof ListModel lm) {
					field = new ListField(accessor, mutator, name, lm);
				} else {
					field = new BeanField(accessor, mutator, name, model);
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
	}

}
