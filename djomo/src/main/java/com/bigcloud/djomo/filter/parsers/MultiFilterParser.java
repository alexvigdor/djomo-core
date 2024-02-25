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

import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.base.BaseParserFilter;
/**
 * Combine multiple FilterParsers in order
 * 
 * @author Alex Vigdor
 *
 */
public class MultiFilterParser extends BaseParserFilter {
	BaseParserFilter[] filters;

	public MultiFilterParser(BaseParserFilter... filters) {
		this.filters = filters;
	}

	@Override
	public void filter(Parser parser) {
		var f = filters;
		for (int i = f.length - 1; i >= 0; i--) {
			ParserFilter fp = f[i];
			fp.filter(parser);
			parser = fp;
		}
		this.parser = parser;
	}

	public MultiFilterParser clone() {
		MultiFilterParser clone = (MultiFilterParser) super.clone();
		var fs = filters.clone();
		clone.filters = fs;
		for (int i = 0; i < fs.length; i++) {
			fs[i] = fs[i].clone();
		}
		return clone;
	}
}