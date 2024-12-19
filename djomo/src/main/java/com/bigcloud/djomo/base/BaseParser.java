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
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.ParserFilterFactory;

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
	
	@Override
	public Object parse(Model definition) {
		return definition.parse(parser);
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

	@Override
	public Models models() {
		return models;
	}
}
