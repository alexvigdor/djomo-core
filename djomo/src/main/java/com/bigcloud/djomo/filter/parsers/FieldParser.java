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

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
import com.bigcloud.djomo.internal.ConcreteType;

/**
 * Selectively apply another FilterParser only when within the specified field
 * of the declared type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to filter by field
 */
public class FieldParser<T> extends BaseParserFilter {
	final FilterFieldObjectModels fieldModels;
	final Class<T> type;

	public FieldParser(String field, ParserFilterFactory filterParser) {
		type = ConcreteType.get(this.getClass(), 0);
		fieldModels = init(field, filterParser);
	}

	public FieldParser(Class<T> type, String field, ParserFilterFactory filterParser) {
		this.type = type;
		fieldModels = init(field, filterParser);
	}

	private FilterFieldObjectModels init(String field, ParserFilterFactory filterParser) {
		return new FilterFieldObjectModels(stream -> stream.map(f -> {
			if (f.key().toString().equals(field)) {
				return new FilterField(f) {
					@Override
					public void parse(Object destination, Parser parser) {
						ParserFilter isolated = filterParser.newParserFilter();
						isolated.filter(parser);
						super.parse(destination, isolated);
					}
				};
			}
			return f;
		}));
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public Object parseObject(ObjectModel model) {
		if (type.isAssignableFrom(model.getType())) {
			model = fieldModels.getFilteredObjectModel(model);
		}
		return parser.parseObject(model);
	}

}
