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

import java.util.Map;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseObjectModel;

/**
 * Base ObjectModel for filtering other ObjectModels, by filtering a stream of
 * fields.
 * 
 * @author Alex Vigdor
 *
 * @param <T> Class of object this model describes
 */
public class FilterFieldObjectModel<T> extends BaseObjectModel<T> implements ObjectModel<T> {
	public FilterFieldObjectModel(ObjectModel<T> objectModel, Stream<Field> fields) {
		this(objectModel, fields.toArray(Field[]::new));
	}

	public FilterFieldObjectModel(ObjectModel<T> objectModel, Field... fields) {
		super(objectModel.models(), objectModel.getType(), fields);
		this.objectModel = objectModel;
	}

	protected final ObjectModel<T> objectModel;

	@Override
	public Object maker(T obj) {
		return objectModel.maker(obj);
	}

	@Override
	public Object maker() {
		return objectModel.maker();
	}

	@Override
	public T make(Object maker) {
		return objectModel.make(maker);
	}

	@Override
	protected Map<CharSequence, Field> initFields(ModelContext context) throws IllegalAccessException {
		return null;
	}

}
