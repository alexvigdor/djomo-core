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

import java.util.function.Supplier;

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.filter.FilterListModel;

/**
 * Limit the number of items parsed into a ListModel. -1 is unlimited.
 * 
 * @author Alex Vigdor
 *
 */
public class LimitParser extends BaseParserFilter {
	protected Supplier<Integer> limitSupplier;

	public LimitParser(String limit) {
		this(Integer.parseInt(limit));
	}

	public LimitParser(int limit) {
		this(() -> limit);
	}

	public LimitParser(Supplier<Integer> limitSupplier) {
		this.limitSupplier = limitSupplier;
	}

	@Override
	public Object parseList(ListModel model) {
		if (!(model instanceof LimitModel)) {
			model = new LimitModel(model, limitSupplier.get());

		}
		return parser.parseList(model);
	}

	@Override
	public Object parse(Model model) {
		if (model instanceof ListModel lm) {
			model = new LimitModel(lm, limitSupplier.get());
		}
		return parser.parse(model);
	}

	static class LimitModel extends FilterListModel {
		int limit;

		public LimitModel(ListModel delegate, int limit) {
			super(delegate);
			this.limit = limit;
		}

		@Override
		public void parseItem(Object listMaker, Parser parser) {
			if (--limit >= 0) {
				listModel.parseItem(listMaker, parser);
			} else {
				listModel.itemModel().parse(parser);
			}
		}
	}
}
