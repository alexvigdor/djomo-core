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

import java.util.List;
import java.util.function.BiConsumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Base ObjectModel for filtering other ObjectModels.
 * 
 * @author Alex Vigdor
 *
 * @param <T> Class of object this model describes
 * @param <M> ObjectMaker used to create an instance of this model
 * @param <F> Field used by this model
 * @param <K> Key class used by this model
 * @param <V> Value class used by this model
 */
public class FilterObjectModel<T, M extends ObjectMaker<T, F, V>, F extends Field<T, K, V>, K, V> extends FilterModel<T>
		implements ObjectModel<T, M, F, K, V> {
	final ObjectModel<T, M, F, K, V> objectModel;

	public FilterObjectModel(ObjectModel<T, M, F, K, V> delegate) {
		super(delegate);
		this.objectModel = delegate;
	}

	@Override
	public M maker(T obj) {
		return objectModel.maker(obj);
	}

	@Override
	public M maker() {
		return objectModel.maker();
	}

	@Override
	public void forEachField(T t, BiConsumer<K, V> consumer) {
		objectModel.forEachField(t, consumer);
	}

	@Override
	public F getField(String name) {
		return objectModel.getField(name);
	}

	@Override
	public List<F> fields() {
		return objectModel.fields();
	}

}
