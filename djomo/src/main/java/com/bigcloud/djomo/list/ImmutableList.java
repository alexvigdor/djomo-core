/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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
package com.bigcloud.djomo.list;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ImmutableList extends AbstractList {
	Object[] items;
	int pointer = 0;

	public int size() {
		return pointer;
	}

	@Override
	public Object get(int index) {
		if (index >= pointer || index < 0) {
			throw new IndexOutOfBoundsException(index);
		}
		return items[index];
	}

	@Override
	public void forEach(Consumer consumer) {
		int p = pointer;
		Object[] os = items;
		for (int i = 0; i < p; i++) {
			consumer.accept(os[i]);
		}
	}

	protected void addItem(Object item) {
		var _items = items;
		var p = pointer;
		if (_items == null) {
			_items = items = new Object[16];
		} else if (p == _items.length) {
			_items = items = Arrays.copyOf(_items, p + p);
		}
		_items[p] = item;
		pointer = p + 1;
	}
}