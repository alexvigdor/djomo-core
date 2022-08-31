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
package com.bigcloud.djomo.filter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.SimpleModel;
/**
 * <p>
 * Base class for FilterParsers that can be applied to Json read operations.
 * </p><p>
 * Default implementation passes through all methods to the underlying parser; 
 * subclasses can override just the parse* methods of interest.  Subclass methods should generally 
 * invoke the corresponding super method on this base class, or on the wrapped parser directly, 
 * unless the intention is to modify the data structure.
 * </p><p>
 * Subclasses MUST provide a proper clone() implementation that invokes super.clone(); FilterParsers
 * loaded from annotations and/or applied to a JsonBuilder are cloned before each invocation, so it is safe
 * to have processing state stored in filter instance fields.
 * </p>
 * 
 * @author Alex Vigdor
 *
 */
public class FilterParser implements Parser, Cloneable{
	protected Parser parser;

	public FilterParser() {
	}

	public FilterParser(Parser parser) {
		this.parser = parser;
	}

	public FilterParser parser(Parser parser) {
		this.parser = parser;
		return this;
	}

	@Override
	public <T> T parse(Model<T> model) {
		return parser.parse(model);
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> M parseObject(ObjectModel<O, M, F,?,V> model) {
		return parser.parseObject(model);
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F,?,V> model, String field, BiConsumer<F, V> consumer) {
		parser.parseObjectField(model, field, consumer);
	}

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> model) {
		return parser.parseList(model);
	}

	@Override
	public <T> void parseListItem(Model<T> model, Consumer<T> consumer) {
		parser.parseListItem(model, consumer);
	}

	@Override
	public <T> T parseSimple(SimpleModel<T> model) {
		return parser.parseSimple(model);
	}

	@Override
	public Object parseNull() {
		return parser.parseNull();
	}

	@Override
	public Models models() {
		return parser.models();
	}
	
	public FilterParser clone() {
		try {
			FilterParser clone = (FilterParser) super.clone();
			clone.parser = null;
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
