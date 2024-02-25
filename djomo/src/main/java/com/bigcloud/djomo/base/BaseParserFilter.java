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

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;

/**
 * <p>
 * Base class for FilterParsers that can be applied to Json read operations.
 * </p>
 * <p>
 * Default implementation passes through all methods to the underlying parser;
 * subclasses can override just the parse* methods of interest. Subclass methods
 * should generally invoke the corresponding super method on this base class, or
 * on the wrapped parser directly, unless the intention is to modify the data
 * structure.
 * </p>
 * <p>
 * Subclasses MUST provide a proper clone() implementation that invokes
 * super.clone(); FilterParsers loaded from annotations and/or applied to a
 * JsonBuilder are cloned before each invocation, so it is safe to have
 * processing state stored in filter instance fields.
 * </p>
 * 
 * @author Alex Vigdor
 *
 */
public class BaseParserFilter implements ParserFilter, Cloneable {
	protected Parser parser;

	public BaseParserFilter() {
	}

	public BaseParserFilter(Parser parser) {
		this.parser = parser;
	}

	public void filter(Parser parser) {
		this.parser = parser;
	}

	@Override
	public Object parse(Model model) {
		return parser.parse(model);
	}

	@Override
	public Object parseObject(ObjectModel model) {
		return parser.parseObject(model);
	}

	@Override
	public Field parseObjectField(ObjectModel model, CharSequence field) {
		return parser.parseObjectField(model, field);
	}

	@Override
	public Object parseList(ListModel model) {
		return parser.parseList(model);
	}

	@Override
	public void parseListItem() {
		parser.parseListItem();
	}

	@Override
	public Object parseNull() {
		return parser.parseNull();
	}

	@Override
	public Models models() {
		return parser.models();
	}

	public BaseParserFilter clone() {
		try {
			BaseParserFilter clone = (BaseParserFilter) super.clone();
			clone.parser = null;
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int parseInt() {
		return parser.parseInt();
	}

	@Override
	public long parseLong() {
		return parser.parseLong();
	}

	@Override
	public float parseFloat() {
		return parser.parseFloat();
	}

	@Override
	public double parseDouble() {
		return parser.parseDouble();
	}

	@Override
	public boolean parseBoolean() {
		return parser.parseBoolean();
	}

	@Override
	public CharSequence parseString() {
		return parser.parseString();
	}

	@Override
	public BaseParserFilter newParserFilter() {
		return clone();
	}
}
