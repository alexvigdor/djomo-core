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

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.error.GetFieldException;
import com.bigcloud.djomo.error.SetFieldException;
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
		this.mutator = mutator;
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
		var a = accessor;
		if (a == null) {
			return null;
		}
		try {
			return a.invoke(o);
		} catch (Throwable e) {
			throw new GetFieldException("Error accessing " + name + " for " + o + " (" + o.getClass().getName() + ")",
					e);
		}
	}

	@Override
	public void set(Object receiver, Object value) {
		var m = mutator;
		if (m == null) {
			return;
		}
		try {
			m.invoke(receiver, value);
		} catch (Throwable e) {
			throw new SetFieldException("Error setting " + name + " = " + value + " for " + receiver + " ("
					+ receiver.getClass().getName() + ")", e);
		}
	}

	@Override
	public void visit(Object source, Visitor visitor) {
		visitor.visitObjectField(key);
		Object val = get(source);
		if (val == null) {
			visitor.visitNull();
		} else {
			model.visit(val, visitor);
		}
	}

	@Override
	public void parse(Object dest, Parser parser) {
		var value = parser.parse(model);
		var m = mutator;
		if (m == null) {
			return;
		}
		try {
			m.invoke(dest, value);
		} catch (Throwable e) {
			throw new SetFieldException(
					"Error setting " + name + " = " + value + " for " + dest + " (" + dest.getClass().getName() + ")",
					e);
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
		public void visit(Object source, Visitor visitor) {
			var a = accessor;
			if (a == null) {
				return;
			}
			visitor.visitObjectField(key);
			try {
				String val = (String) a.invoke(source);
				if (val == null) {
					visitor.visitNull();
				} else {
					visitor.visitString(val);
				}
			} catch (Throwable e) {
				throw new GetFieldException(
						"Error accessing " + name + " for " + source + " (" + source.getClass().getName() + ")", e);
			}

		}

		@Override
		public void parse(Object dest, Parser parser) {
			CharSequence value = parser.parseString();
			var m = mutator;
			if (m == null) {
				return;
			}
			try {
				if(value != null) {
					value = value.toString();
				}
				m.invoke(dest, value);
			} catch (Throwable e) {
				throw new SetFieldException("Error setting " + name + " = " + value + " for " + dest + " ("
						+ dest.getClass().getName() + ")", e);
			}
		}

	}

	public static class DoubleField extends BeanField {

		public DoubleField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			var a = accessor;
			if (a == null) {
				return;
			}
			visitor.visitObjectField(key);
			double val;
			try {
				val = (double) a.invoke(source);
			} catch (Throwable e) {
				throw new GetFieldException(
						"Error accessing " + name + " for " + source + " (" + source.getClass().getName() + ")", e);
			}
			visitor.visitDouble(val);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			double value = parser.parseDouble();
			var m = mutator;
			if (m == null) {
				return;
			}
			try {
				m.invoke(dest, value);
			} catch (Throwable e) {
				throw new SetFieldException("Error setting " + name + " = " + value + " for " + dest + " ("
						+ dest.getClass().getName() + ")", e);
			}
		}
	}

	public static class FloatField extends BeanField {

		public FloatField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			var a = accessor;
			if (a == null) {
				return;
			}
			visitor.visitObjectField(key);
			float val;
			try {
				val = (float) a.invoke(source);
			} catch (Throwable e) {
				throw new GetFieldException(
						"Error accessing " + name + " for " + source + " (" + source.getClass().getName() + ")", e);
			}
			visitor.visitFloat(val);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			float value = parser.parseFloat();
			var m = mutator;
			if (m == null) {
				return;
			}
			try {
				m.invoke(dest, value);
			} catch (Throwable e) {
				throw new SetFieldException("Error setting " + name + " = " + value + " for " + dest + " ("
						+ dest.getClass().getName() + ")", e);
			}
		}
	}

	public static class LongField extends BeanField {

		public LongField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			var a = accessor;
			if (a == null) {
				return;
			}
			visitor.visitObjectField(key);
			long val;
			try {
				val = (long) a.invoke(source);
			} catch (Throwable e) {
				throw new GetFieldException(
						"Error accessing " + name + " for " + source + " (" + source.getClass().getName() + ")", e);
			}
			visitor.visitLong(val);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			long value = parser.parseLong();
			var m = mutator;
			if (m == null) {
				return;
			}
			try {
				m.invoke(dest, value);
			} catch (Throwable e) {
				throw new SetFieldException("Error setting " + name + " = " + value + " for " + dest + " ("
						+ dest.getClass().getName() + ")", e);
			}
		}
	}

	public static class IntField extends BeanField {

		public IntField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			var a = accessor;
			if (a == null) {
				return;
			}
			visitor.visitObjectField(key);
			int val;
			try {
				val = (int) a.invoke(source);
			} catch (Throwable e) {
				throw new GetFieldException(
						"Error accessing " + name + " for " + source + " (" + source.getClass().getName() + ")", e);
			}
			visitor.visitInt(val);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			int value = parser.parseInt();
			var m = mutator;
			if (m == null) {
				return;
			}
			try {
				m.invoke(dest, value);
			} catch (Throwable e) {
				throw new SetFieldException("Error setting " + name + " = " + value + " for " + dest + " ("
						+ dest.getClass().getName() + ")", e);
			}
		}
	}

	public static class BooleanField extends BeanField {

		public BooleanField(MethodHandle accessor, MethodHandle mutator, String name, Model model) {
			super(accessor, mutator, name, model);
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			var a = accessor;
			if (a == null) {
				return;
			}
			visitor.visitObjectField(key);
			boolean val;
			try {
				val = (boolean) a.invoke(source);
			} catch (Throwable e) {
				throw new GetFieldException(
						"Error accessing " + name + " for " + source + " (" + source.getClass().getName() + ")", e);
			}
			visitor.visitBoolean(val);
		}

		@Override
		public void parse(Object dest, Parser parser) {
			boolean value = parser.parseBoolean();
			var m = mutator;
			if (m == null) {
				return;
			}
			try {
				m.invoke(dest, value);
			} catch (Throwable e) {
				throw new SetFieldException("Error setting " + name + " = " + value + " for " + dest + " ("
						+ dest.getClass().getName() + ")", e);
			}
		}
	}

	public static class Builder {
		private MethodHandle accessor;
		private MethodHandle mutator;
		private String name;
		private Model model;

		public BeanField build() {
			if (model.getType() == String.class) {
				return new StringField(accessor, mutator, name, model);
			} else if (model.getType() == double.class) {
				return new DoubleField(accessor, mutator, name, model);
			} else if (model.getType() == float.class) {
				return new FloatField(accessor, mutator, name, model);
			} else if (model.getType() == int.class) {
				return new IntField(accessor, mutator, name, model);
			} else if (model.getType() == long.class) {
				return new LongField(accessor, mutator, name, model);
			} else if (model.getType() == boolean.class) {
				return new BooleanField(accessor, mutator, name, model);
			}
			return new BeanField(accessor, mutator, name, model);
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
