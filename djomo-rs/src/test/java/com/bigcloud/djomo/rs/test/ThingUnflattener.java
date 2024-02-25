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
package com.bigcloud.djomo.rs.test;

import java.util.List;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.parsers.ModelParser;

public class ThingUnflattener implements ModelParser {

	@Override
	public Object parse(Model model, Parser parser) {
		if (model.getType() == Thing.class) {
			Object obj = parser.parse(parser.models().listModel);
			List list = (List) obj;
			return new Thing(list.get(0).toString(), list.subList(1, list.size()));
		}
		return parser.parse(model);
	}
}
