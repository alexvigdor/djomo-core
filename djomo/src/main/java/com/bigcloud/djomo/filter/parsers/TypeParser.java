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
package com.bigcloud.djomo.filter.parsers;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Selectively apply another FilterParser only when parsing a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the wrapped filter should be applied
 */
public class TypeParser<T> extends BaseParserFilter {
	final Class<T> type;
	ParserFilter filterParser;
	Parser target;

	public TypeParser(ParserFilter filterParser) {
		type = ConcreteType.get(this.getClass(), 0);
		this.filterParser = filterParser;
	}

	protected TypeParser(int index, ParserFilter filterParser) {
		type = ConcreteType.get(this.getClass(), index);
		this.filterParser = filterParser;
	}

	public TypeParser(Class<T> type, ParserFilter filterParser) {
		this.type = type;
		this.filterParser = filterParser;
	}

	public Class<T> getType() {
		return type;
	}

	public TypeParser<T> clone() {
		TypeParser<T> clone = (TypeParser<T>) super.clone();
		clone.filterParser = filterParser.newParserFilter();
		return clone;
	}

	@Override
	public void filter(Parser parser) {
		this.target = parser;
		var fp = filterParser;
		if (this.parser != fp) {
			this.parser = parser;
		}
		fp.filter(parser);
	}

	@Override
	public Object parse(Model model) {
		var op = parser;
		Parser p;
		if (type == Object.class || model != null && type.isAssignableFrom(model.getType())) {
			parser = p = filterParser;
		} else {
			parser = p = target;
		}
		var t = p.parse(model);
		this.parser = op;
		return t;
	}

}
