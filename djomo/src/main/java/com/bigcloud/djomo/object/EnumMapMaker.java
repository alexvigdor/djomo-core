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

import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.base.BaseMaker;

public class EnumMapMaker<T extends EnumMap<E, V>, E extends Enum<E>, V> extends BaseMaker<T, EnumMapModel<T, E, V>> implements ObjectMaker<T, EnumMapField<T, E, V>, V> {
	final T map;

	public EnumMapMaker(EnumMapModel<T, E, V> model) {
		super(model);
		this.map = model.newInstance();
	}

	@Override
	public T make() {
		return map;
	}

	@Override
	public void field(EnumMapField<T, E, V> field, V value) {
		map.put(field.key, value);
	}

}
