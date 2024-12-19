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
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.io.Buffer;

public class MergeJsonParser extends JsonParser {
	public MergeJsonParser(Models context, Buffer input, Buffer overflow, Object destination,
			ParserFilterFactory... filters) {
		super(context, input, overflow, filters);
		this.source = destination;
	}

	protected Object object;
	protected Object source;
	protected Object list;

	@Override
	protected Object objectMaker(ObjectModel definition) {
		if (object != null) {
			return definition.maker(object);
		}
		return definition.maker();
	}

	@Override
	protected Object listMaker(ListModel definition) {
		if (list != null) {
			var maker = definition.maker(list);
			list = null;
			return maker;
		}
		return definition.maker();
	}

	@Override
	public Field parseObjectField(ObjectModel model, CharSequence field) {
		Field f = super.parseObjectField(model, field);
		Object o = object;
		if (f == null || o == null) {
			source = null;
		} else {
			source = f.get(o);
		}
		return f;
	}

	@Override
	public Object parseList(ListModel definition) {
		var ls = list;
		list = source;
		source = null;
		var pl = super.parseList(definition);
		list = ls;
		return pl;
	}

	@Override
	public Object parseObject(ObjectModel definition) {
		var ro = object;
		object = source;
		source = null;
		var po = super.parseObject(definition);
		object = ro;
		return po;
	}

	@Override
	public Object parseNull() {
		super.parseNull();
		return source;
	}

}