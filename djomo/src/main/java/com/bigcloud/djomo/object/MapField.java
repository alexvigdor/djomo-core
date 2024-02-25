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

import java.util.Map;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;

public class MapField implements Field {
	final Object name;
	final Object key;
	final Model model;

	public MapField(Object name, Model model) {
		this.name = name;
		this.key = name;
		this.model = model;
	}

	private MapField(Object name, Object key, Model model) {
		this.name = name;
		this.key = key;
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
		return ((Map) o).get(name);
	}

	@Override
	public void set(Object destination, Object value) {
		((Map) destination).put(name, value);
	}

	@Override
	public void visit(Object source, Visitor visitor) {
		visitor.visitObjectField(key);
		model.visit(((Map) source).get(name), visitor);
	}

	@Override
	public void parse(Object dest, Parser parser) {
		var value = parser.parse(model);
		((Map) dest).put(name, value);
	}

	@Override
	public Field rekey(Object newKey) {
		return new MapField(name, newKey, model);
	}
}
