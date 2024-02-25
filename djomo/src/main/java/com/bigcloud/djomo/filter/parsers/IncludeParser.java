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

import java.util.Objects;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
/**
 * Define a subset of fields from the given model that will be pulled through the parser.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type to filter by only parsing the defined fields
 */
public class IncludeParser<T> extends BaseParserFilter {
	final FilterFieldObjectModels includeModels;
	final Class<T> type;

	public IncludeParser(Class<T> type, String... fields) {
		includeModels = new FilterFieldObjectModels(model -> Stream.of(fields).map(model::getField).filter(Objects::nonNull));
		this.type = type;
	}

	@Override
	public Object parse(Model model) {
		if (type.isAssignableFrom(model.getType()) && model instanceof ObjectModel om) {
			return parser.parse(includeModels.getFilteredObjectModel(om));
		} else {
			return parser.parse(model);
		}
	}
}
