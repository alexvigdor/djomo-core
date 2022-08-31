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
import java.util.stream.Stream;

import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
/**
 * Limit the number of items visited in a ListModel.  -1 is unlimited.
 * 
 * @author Alex Vigdor
 *
 */
public class LimitVisitor extends FilterVisitor {
	protected Supplier<Integer> limitSupplier;

	public LimitVisitor(String limit) {
		this(Integer.parseInt(limit));
	}
	public LimitVisitor(int limit) {
		this(() -> limit);
	}

	public LimitVisitor(Supplier<Integer> limitSupplier) {
		this.limitSupplier = limitSupplier;
	}

	LimitModel limitModel;

	@Override
	public <T> void visitList(T model, ListModel<T, ?, ?> definition) {
		LimitModel om = limitModel;
		limitModel = new LimitModel<>(definition);
		visitor.visitList(model, limitModel);
		limitModel = om;
	}

	@Override
	public void visitListItem(Object obj) {
		visitor.visitListItem(obj);
		limitModel.remaining--;
	}
	public class LimitModel<T, M extends ListMaker<T, I>, I> extends FilterListModel<T, M, I> {
		int remaining;

		public LimitModel(ListModel<T, M, I> delegate) {
			super(delegate);
			this.remaining = limitSupplier.get();
		}

		@Override
		public void forEachItem(T t, Consumer<I> consumer) {
			Stream<I> items = listModel.stream(t);
			items.takeWhile(i -> remaining != 0).forEach(consumer);
		}

	}
}
