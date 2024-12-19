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

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.api.ParserTypedFilterFactory;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.internal.ConcreteType;

@FunctionalInterface
public interface ObjectParser<M> extends ParserFilterFactory, ParserTypedFilterFactory {
	M parseObject(ObjectModel<M> model, Parser parser);

	@Override
	default ParserFilter newParserFilter() {
		return ConcreteType.tryGet(this.getClass(), 0)
				.map(this::newParserFilter)
				.orElseGet(() -> new BaseParserFilter() {
					@Override
					public Object parseObject(ObjectModel model) {
						return ObjectParser.this.parseObject(model, parser);
					}

				});
	}

	@Override
	default ParserFilter newParserFilter(Class<?> boundingType) {
		return new BaseParserFilter() {
			@Override
			public Object parseObject(ObjectModel model) {
				if (boundingType.isAssignableFrom(model.getType())) {
					return ObjectParser.this.parseObject(model, parser);
				}
				return parser.parseObject(model);
			}

		};
	}
}
