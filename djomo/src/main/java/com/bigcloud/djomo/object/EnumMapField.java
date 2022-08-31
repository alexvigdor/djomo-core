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
package com.bigcloud.djomo.object;

import java.util.EnumMap;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;

public class EnumMapField<T extends EnumMap<E, V>, E extends Enum<E>, V> implements Field<T, E, V> {
	final E key;
	final Model<V> model;
	
	public EnumMapField(E key, Model<V> model) {
		this.key = key;
		this.model = model;
	}

	@Override
	public E key() {
		return key;
	}

	@Override
	public Model<V> model() {
		return model;
	}

	@Override
	public V get(T o) {
		return o.get(key);
	}

}
