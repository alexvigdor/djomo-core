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

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
/**
 * Base model for filtering other models
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of model to filter
 */
public class FilterModel<T> implements Model<T> {
	final Model<T> delegate;

	public FilterModel(Model<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Class<T> getType() {
		return delegate.getType();
	}

	@Override
	public T convert(Object o) {
		return delegate.convert(o);
	}

	@Override
	public T parse(Parser parser) {
		return delegate.parse(parser);
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		delegate.visit(obj, visitor);
	}

	@Override
	public Format getFormat() {
		return delegate.getFormat();
	}

	@Override
	public Models models() {
		return delegate.models();
	}

}
