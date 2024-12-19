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
package com.bigcloud.djomo.filter;

import java.util.Map;

import com.bigcloud.djomo.api.ObjectModel;

public class RenameFieldModels {
	FilterFieldObjectModels filterFieldObjectModels;

	public RenameFieldModels(Map<String, String> mappings) {
		this.filterFieldObjectModels = new FilterFieldObjectModels(stream -> stream
				.map(field -> {
					String name = mappings.get(field.key().toString());
					if (name != null) {
						return field.rekey(name);
					}
					return field;
				}));
	}

	public ObjectModel getFilteredFieldModel(ObjectModel model) {
		return filterFieldObjectModels.getFilteredObjectModel(model);
	}
}
