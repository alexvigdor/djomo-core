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
package com.bigcloud.djomo.filter.parsers;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.internal.ConcreteType;

/**
 * Selectively apply custom parsing logic only when parsing a given type.
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to which the custom parsing should be applied
 */
public abstract class TypeParser<T> extends BaseParserFilter {
	final Class<T> type;

	public TypeParser() {
		type = ConcreteType.get(this.getClass(), 0);
	}

	protected TypeParser(int index) {
		type = ConcreteType.get(this.getClass(), index);
	}

	public TypeParser(Class<T> type) {
		this.type = type;
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public Object parse(Model model) {
		if (type == Object.class || model != null && type.isAssignableFrom(model.getType())) {
			return parseType(model);
		} else {
			return parser.parse(model);
		}
	}

	public abstract T parseType(Model<T> model);

}
