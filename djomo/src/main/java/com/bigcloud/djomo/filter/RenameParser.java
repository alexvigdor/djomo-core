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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
/**
 * Rename fields from what is found in the Json source, to how the Model Field is named.
 * 
 * @author Alex Vigdor
 *
 */
public class RenameParser extends FilterParser {
	private final ConcurrentHashMap<CharSequence, CharSequence> nameMappings;

	public RenameParser(String... args) {
		this.nameMappings = new ConcurrentHashMap<>();
		for (int i = 0; i < (args.length - 1); i += 2) {
			nameMappings.put(args[i], args[i + 1]);
		}
	}

	public RenameParser(Map<String, String> nameMappings) {
		this.nameMappings = new ConcurrentHashMap<>(nameMappings);
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F, ?, V> model, CharSequence field, BiConsumer<F, V> consumer) {
		parser.parseObjectField(model, nameMappings.getOrDefault(field, field), consumer);
	}

}
