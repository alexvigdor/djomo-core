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
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.simple.BooleanModel;
import com.bigcloud.djomo.simple.NumberModel;
import com.bigcloud.djomo.simple.StringModel;

/**
 * Baseline parser support, source agnostic
 * 
 * @author Alex Vigdor
 *
 */
public abstract class BaseParser implements Parser {
	protected final Models models;
	protected final Parser parser;

	public BaseParser(Models models, ParserFilterFactory... filters) {
		this.models = models;
		Parser end = this;
		if (filters != null) {
			for (int i = 0; i < filters.length; i++) {
				ParserFilter filter = filters[i].newParserFilter();
				filter.filter(end);
				end = filter;
			}
		}
		this.parser = end;
	}

	protected Object parseObjectModel(Model model) {
		if (model instanceof ObjectModel || model.getFormat() == Format.OBJECT) {
			return model.parse(parser);
		}
		return models.mapModel.parse(parser);
	}

	protected Object parseListModel(Model model) {
		if (model instanceof ListModel || model.getFormat() == Format.LIST) {
			return model.parse(parser);
		}
		return models.listModel.parse(parser);
	}

	protected Object parseStringModel(Model model) {
		if (model instanceof StringModel) {
			return parser.parseString().toString();
		}
		return switch (model.getFormat()) {
		case STRING -> model.parse(parser);
		case NUMBER, BOOLEAN -> model.convert(parser.parseString().toString());
		default -> parser.parseString().toString();
		};
	}

	protected Object parseNumberModel(Model model) {
		if (model instanceof NumberModel || model.getFormat() == Format.NUMBER) {
			return model.parse(parser);
		}
		double d = parser.parseDouble();
		if (Math.rint(d) == d && Double.isFinite(d)) {
			// Down convert to the simplest representation
			int i = (int) d;
			if (i == d) {
				return i;
			}
			return (long) d;
		}
		return d;
	}

	protected <T> T parseBooleanModel(Model<T> model) {
		if (model instanceof BooleanModel || model.getFormat() == Format.BOOLEAN) {
			return model.parse(parser);
		}
		return (T) (Boolean) parser.parseBoolean();
	}

	protected <T> T parseNullModel(Model<T> model) {
		return model.convert(parseNull());
	}

	protected Object objectMaker(ObjectModel definition) {
		return definition.maker();
	}

	protected Object listMaker(ListModel definition) {
		return definition.maker();
	}

	@Override
	public void parseListItem() {
	}

	/**
	 * Entry point to allow filters to intercept top-level object visit; subclasses
	 * should extend visit as opposed to filter
	 * 
	 */
	public final <T> T filter(Model<T> model) {
		return (T) parser.parse(model);
	}

	@Override
	public Models models() {
		return models;
	}
}
