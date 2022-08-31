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
import com.bigcloud.djomo.error.GetFieldException;
import com.bigcloud.djomo.error.SetFieldException;

public class BeanField<T, V> implements Field<T, String, V> {
	private final MethodHandle accessor;
	private final MethodHandle mutator;
	private final String name;
	private final Model<V> model;
	
	public BeanField(MethodHandle accessor, MethodHandle mutator, String name, Model<V> model) {
		this.accessor = accessor;
		this.mutator = mutator;
		this.name = name;
		this.model = model;
	}

	@Override
	public String key() {
		return name;
	}

	@Override
	public Model<V> model() {
		return model;
	}

	@Override
	public V get(T o) {
		var a = accessor;
		if (a == null) {
			return null;
		}
		try {
			return (V) a.invoke(o);
		} catch (Throwable e) {
			throw new GetFieldException("Error accessing " + name + " for " + o + " (" + o.getClass().getName() + ")",
					e);
		}
	}

	public void set(Object receiver, V value) {
		var m = mutator;
		if(m==null) {
			return;
		}
		try {
			m.invoke(receiver, value);
		} catch (Throwable e) {
			throw new SetFieldException("Error setting " + name + " = " + value + " for " + receiver + " ("
					+ receiver.getClass().getName() + ")", e);
		}
	}

	public static <T, V> Builder<T, V> builder() {
		return new Builder<>();
	}

	public static class Builder<T, V> {
		private MethodHandle accessor;
		private MethodHandle mutator;
		private String name;
		private Model<V> model;

		public BeanField<T, V> build() {
			return new BeanField<>(accessor, mutator, name, model);
		}

		public Builder<T, V> mutator(MethodHandle mutator) {
			this.mutator = mutator;
			return this;
		}

		public Builder<T, V> accessor(MethodHandle accessor) {
			this.accessor = accessor;
			return this;
		}

		public Builder<T, V> model(Model<V> model) {
			this.model = model;
			return this;
		}

		public Builder<T, V> name(String name) {
			this.name = name;
			return this;
		}
	}

}
