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
/**
 * Selectively apply another FilterParser only when parsing a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the wrapped filter should be applied
 */
public class TypeParserFilter<T> extends TypeParser<T> {
	ParserFilter filterParser;
	Parser target;

	public TypeParserFilter(ParserFilter filterParser) {
		super();
		this.filterParser = filterParser;
	}

	protected TypeParserFilter(int index, ParserFilter filterParser) {
		super(index);
		this.filterParser = filterParser;
	}

	public TypeParserFilter(Class<T> type, ParserFilter filterParser) {
		super(type);
		this.filterParser = filterParser;
	}

	public TypeParserFilter<T> clone() {
		TypeParserFilter<T> clone = (TypeParserFilter<T>) super.clone();
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
	public T parseType(Model<T> model) {
		var fp = filterParser;
		var filtered = parser == fp;
		if(!filtered) {
			parser = fp;
		}
		var t = fp.parse(model);
		if(!filtered) {
			parser = target;
		}
		return  (T) t;
	}

}
