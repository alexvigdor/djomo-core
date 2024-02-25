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

import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.filter.FilterListModel;
/**
 * Prevent null values from reaching list  makers.
 * 
 * @author Alex Vigdor
 *
 */
public class OmitNullItemParser extends BaseParserFilter {

	@Override
	public Object parseList(ListModel model) {
		return parser.parseList(new FilterListModel<>(model) {
			@Override
			public void parseItem(Object listMaker, Parser parser) {
				var result = parser.parse(itemModel());
				if(result == null) {
					return;
				}
				ParserFilter filter = Filters.parseModel((model, p) -> result);
				filter.filter(parser);
				model.parseItem(listMaker, filter);
			}
		});
	}


}
