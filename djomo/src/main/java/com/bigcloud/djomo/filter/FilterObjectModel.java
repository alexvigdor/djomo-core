/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;

public class FilterObjectModel<O> extends FilterModel<O> implements ObjectModel<O> {
	protected final ObjectModel<O> objectModel;
	public FilterObjectModel(ObjectModel<O> delegate) {
		super(delegate);
		this.objectModel = delegate;
	}

	@Override
	public void forEachField(O t, BiConsumer consumer) {
		objectModel.forEachField(t, consumer);
	}

	@Override
	public void visitFields(O t, Visitor visitor) {
		objectModel.visitFields(t, visitor);
	}

	@Override
	public Field getField(CharSequence name) {
		return objectModel.getField(name);
	}

	@Override
	public List<Field> fields() {
		return objectModel.fields();
	}
	
	@Override
	public Stream<Field> fields(O obj) {
		return objectModel.fields(obj);
	}

	@Override
	public Object maker(O obj) {
		return objectModel.maker(obj);
	}

	@Override
	public Object maker() {
		return objectModel.maker();
	}

	@Override
	public O make(Object maker) {
		return objectModel.make(maker);
	}

}
