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

import java.util.Set;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.filter.FilterFieldObjectModels;
/**
 * Prevents parsed field values from being loaded into the model.
 * 
 * @author Alex Vigdor
 *
 */
public class ExcludeParser extends BaseParserFilter {
	final FilterFieldObjectModels excludeModels;
	final Class<?> type;

	public ExcludeParser(Class<?> type, String... fields) {
		Set<String> excludes = Set.of(fields);
		excludeModels = new FilterFieldObjectModels(stream -> stream.filter(f -> !excludes.contains(f.key().toString())));
		this.type = type;
	}
	
	@Override
	public Object parseObject(ObjectModel model) {
		if (type.isAssignableFrom(model.getType())) {
			model = excludeModels.getFilteredObjectModel(model);
		} 
		return parser.parseObject(model);
	}
	
}
