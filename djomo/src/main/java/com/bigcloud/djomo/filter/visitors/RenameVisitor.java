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
package com.bigcloud.djomo.filter.visitors;

import java.util.HashMap;
import java.util.Map;

import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.RenameFieldModels;

/**
 * Rename fields from how the Model Field is named, to what is found in the Json
 * output.
 * 
 * @author Alex Vigdor
 *
 */
public class RenameVisitor extends BaseVisitorFilter {
	final RenameFieldModels renameModels;
	final Class<?> type;

	public RenameVisitor(Class<?> type, String... args) {
		this.type = type;
		Map<String, String> mappings = new HashMap<>();
		for (int i = 0; i < (args.length - 1); i += 2) {
			mappings.put(args[i], args[i + 1]);
		}
		this.renameModels = new RenameFieldModels(mappings);
	}

	@Override
	public <O> void visitObject(O obj, ObjectModel<O> model) {
		if (type.isInstance(obj)) {
			visitor.visitObject(obj, renameModels.getFilteredFieldModel(model));
		} else {
			visitor.visitObject(obj, model);
		}
	}

}
