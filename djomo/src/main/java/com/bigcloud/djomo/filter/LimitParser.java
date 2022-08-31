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

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
/**
 * Limit the number of items parsed into a ListModel.  -1 is unlimited.
 * 
 * @author Alex Vigdor
 *
 */
public class LimitParser extends FilterParser {
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

	int limit = 0;

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> model) {
		var oldLimit = limit;
		limit = limitSupplier.get();
		try {
			return parser.parseList(model);
		} finally {
			limit = oldLimit;
		}
	}

	@Override
	public <T> void parseListItem(Model<T> model, Consumer<T> consumer) {
		parser.parseListItem(model, o -> {
			if (limit != 0) {
				consumer.accept(o);
				if (limit > 0) {
					--limit;
				}
			}
		});
	}
}
