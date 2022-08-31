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

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.internal.ConcreteType;
/**
 * Selectively apply another FilterParser only when within the specified field of the declared type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to filter by field
 */
public class FieldParser<T> extends FilterParser {
	final Class<T> type;
	final String field;
	FilterParser filterParser;
	Parser target;
	boolean typeMatch;

	public FieldParser(String field, FilterParser filterParser) {
		type = ConcreteType.get(this.getClass(), 0);
		this.field = field;
		this.filterParser = filterParser;
	}

	public FieldParser(Class<T> type, String field, FilterParser filterParser) {
		this.type = type;
		this.field = field;
		this.filterParser = filterParser;
	}

	public Class<T> getType() {
		return type;
	}

	public FieldParser<T> clone() {
		FieldParser<T> clone = (FieldParser<T>) super.clone();
		clone.filterParser = filterParser.clone();
		return clone;
	}

	@Override
	public FieldParser<T> parser(Parser parser) {
		this.target = parser;
		var fp = filterParser;
		if (this.parser != fp) {
			this.parser = parser;
		}
		fp.parser(parser);
		return this;
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> M parseObject(
			ObjectModel<O, M, F, ?, V> model) {
		var otm = typeMatch;
		typeMatch = type.isAssignableFrom(model.getType());
		var result = parser.parseObject(model);
		typeMatch = otm;
		return result;
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F, ?, V> model, String field, BiConsumer<F, V> consumer) {
		Parser op = parser;
		Parser dest;
		if (typeMatch && this.field.equals(field)) {
			parser = dest = filterParser;
		} else {
			parser = dest = target;
		}
		dest.parseObjectField(model, field, consumer);
		parser = op;
	}

}
