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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Prevent null values from reaching list or object makers.
 * 
 * @author Alex Vigdor
 *
 */
public class OmitNullParser extends FilterParser {

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F, ?, V> model, String field, BiConsumer<F, V> consumer) {
		parser.parseObjectField(model, field, (f, v) -> {
			if (v != null) {
				consumer.accept(f, v);
			}
		});
	}

	@Override
	public <T> void parseListItem(Model<T> model, Consumer<T> consumer) {
		parser.parseListItem(model, v -> {
			if (v != null) {
				consumer.accept(v);
			}
		});
	}
}
