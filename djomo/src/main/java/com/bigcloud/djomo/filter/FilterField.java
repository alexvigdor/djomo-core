/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;

public abstract class FilterField implements Field, Cloneable {
	Field field;

	public FilterField(Field field) {
		this.field = field;
	}

	@Override
	public Object key() {
		return field.key();
	}

	@Override
	public Model model() {
		return field.model();
	}

	@Override
	public Object get(Object source) {
		return field.get(source);
	}

	@Override
	public void set(Object destination, Object value) {
		field.set(destination, value);
	}

	@Override
	public void visit(Object source, Visitor visitor) {
		field.visit(source, visitor);
	}

	@Override
	public void parse(Object destination, Parser parser) {
		field.parse(destination, parser);
	}

	@Override
	public Field rekey(Object newKey) {
		FilterField cloned = clone();
		cloned.field = field.rekey(newKey);
		return cloned;
	}

	public FilterField clone() {
		try {
			FilterField f = (FilterField) super.clone();
			return f;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

}
