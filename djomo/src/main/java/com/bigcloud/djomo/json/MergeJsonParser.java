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
package com.bigcloud.djomo.json;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ComplexModel;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Maker;
import com.bigcloud.djomo.filter.FilterParser;
import com.bigcloud.djomo.io.Buffer;

public class MergeJsonParser extends JsonParser {
	public MergeJsonParser(Models context, Buffer input, Buffer overflow, Object destination, FilterParser... filters) {
		super(context, input, overflow, filters);
		this.source = destination;
	}

	protected Object source;
	protected Object list;
	@Override
	protected <O, M extends Maker<O>> M maker(ComplexModel<O, M> definition){
		if (source != null) {
			return definition.maker((O) source);
		}
		if (list != null) {
			var maker = definition.maker((O) list);
			list = null;
			return maker;
		}
		return definition.maker();
	}
	@Override
	protected Object parseFieldValue(Field field) {
		Object o = source;
		if (o != null) {
			source = field.get(o);
		}
		try {
			return super.parseFieldValue(field);
		} finally {
			source = o;
		}
	}

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> definition) {
		list = source;
		source = null;
		return super.parseList(definition);
	}

	@Override
	public Object parseNull() {
		super.parseNull();
		return source;
	}
}