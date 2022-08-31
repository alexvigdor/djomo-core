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

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Selectively apply another FilterParser only when parsing a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the wrapped filter should be applied
 */
public class TypeParser<T> extends FilterParser {
	final Class<T> type;
	FilterParser filterParser;
	Parser target;

	public TypeParser(FilterParser filterParser) {
		type = ConcreteType.get(this.getClass(), 0);
		this.filterParser = filterParser;
	}

	protected TypeParser(int index, FilterParser filterParser) {
		type = ConcreteType.get(this.getClass(), index);
		this.filterParser = filterParser;
	}

	public TypeParser(Class<T> type, FilterParser filterParser) {
		this.type = type;
		this.filterParser = filterParser;
	}

	public Class<T> getType() {
		return type;
	}

	public TypeParser<T> clone() {
		TypeParser<T> clone = (TypeParser<T>) super.clone();
		clone.filterParser = filterParser.clone();
		return clone;
	}

	@Override
	public TypeParser<T> parser(Parser parser) {
		this.target = parser;
		var fp = filterParser;
		if (this.parser != fp) {
			this.parser = parser;
		}
		fp.parser(parser);
		return this;
	}

	@Override
	public <M> M parse(Model<M> model) {
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
