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

import java.util.Set;
import java.util.function.BiConsumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Prevents parsed field values from being loaded into the model.  Can be wrapped in a {@link TypeParser} or {@link PathParser}.
 * 
 * @author Alex Vigdor
 *
 */
public class ExcludeParser extends FilterParser {
	Set<String> excludes;

	public ExcludeParser(String... fields) {
		excludes = Set.of(fields);
	}

	public boolean exclude(String fieldName) {
		if (excludes.isEmpty()) {
			return true;
		}
		return excludes.contains(fieldName);
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F, ?, V> model, CharSequence field, BiConsumer<F, V> consumer) {
		super.parseObjectField(model, field, (f, o) -> {
			if (!exclude(f.key().toString())) {
				consumer.accept(f, o);
			}
		});
	}
}
