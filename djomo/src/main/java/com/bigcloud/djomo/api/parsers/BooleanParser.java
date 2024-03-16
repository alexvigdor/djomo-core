/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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
package com.bigcloud.djomo.api.parsers;

import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.base.BaseParserFilter;

@FunctionalInterface
public interface BooleanParser extends ParserFilterFactory {
	boolean parseBoolean(Parser parser);

	@Override
	default ParserFilter newParserFilter() {
		return new BaseParserFilter() {
			@Override
			public boolean parseBoolean() {
				return BooleanParser.this.parseBoolean(parser);
			}

		};
	}
}