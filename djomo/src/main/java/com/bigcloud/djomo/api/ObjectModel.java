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
package com.bigcloud.djomo.api;

import java.util.List;
import java.util.function.BiConsumer;
/**
 * An ObjectModel allows iterating the fields of an object, getting a Field by name to use with a Maker, 
 * or getting a List of Fields if it is predetermined.
 * 
 * @author Alex Vigdor
 *
 * @param <T> Class of object this model describes
 * @param <M> ObjectMaker used to create an instance of this model
 * @param <F> Field used by this model
 * @param <K> Key class used by this model (use String for normal java beans)
 * @param <V> Value class used by this model (use Object if Fields have different types)
 */
public interface ObjectModel<T, M extends ObjectMaker<T, F, V>, F extends Field<T, K, V>, K, V> extends ComplexModel<T, M> {
	void forEachField(T t, BiConsumer<K, V> consumer);
	F getField(CharSequence name);
	/**
	 * return iterable if set of fields is fixed and known; otherwise return null to indicate dynamic field model
	 * @return
	 */
	List<F> fields();
}
