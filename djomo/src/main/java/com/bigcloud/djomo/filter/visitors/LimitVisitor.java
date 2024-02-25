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
package com.bigcloud.djomo.filter.visitors;

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.bigcloud.djomo.ModelType;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;
/**
 * Limit the number of items visited in a ListModel.  -1 is unlimited.
 * 
 * @author Alex Vigdor
 *
 */
public class LimitVisitor extends BaseVisitorFilter {
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

	@Override
	public <T> void visitList(T model, ListModel<T> definition) {
		var streamModel = ModelType.of(Stream.class, definition.itemModel().getType());
		visitor.visitList(definition.stream(model).limit(limitSupplier.get()), models().get(streamModel));
	}


}
