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

import java.util.Map;

import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.base.BaseMaker;

public class MapMaker<T extends Map<K, V>, K, V> extends BaseMaker<T, MapModel<T, K, V>> implements ObjectMaker<T, MapField<T, K, V>, V> {
	final T map;
	
	public MapMaker(MapModel<T, K, V> model) {
		super(model);
		this.map = model.newInstance();
	}

	@Override
	public T make() {
		return map;
	}

	@Override
	public void field(MapField<T, K, V> field, V value) {
		map.put(field.key, value);
	}

}
