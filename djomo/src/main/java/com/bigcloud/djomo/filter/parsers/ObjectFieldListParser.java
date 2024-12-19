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

import java.util.List;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.filter.FilterListModel;
/**
 * 
 * Parse a list of field values into an object model; designed for symmetric use with 
 * ObjectFieldListVisitor
 * 
 * @author Alex Vigdor
 *
 */
public class ObjectFieldListParser extends BaseParserFilter {

	@Override
	public Object parseObject(ObjectModel model) {
		var fields = model.fields();
		if (fields != null) {
			var listParserModel = new ListParserModel(model, fields);
			return parser.parse(listParserModel);
		}
		return parser.parseObject(model);
	}

	private static class ListParserModel<T> extends FilterListModel<T> {
		final ObjectModel model;
		final Object maker;
		final int len;
		final List<? extends Field> fields;
		int pos = 0;
		Field currentField;

		ListParserModel(ObjectModel<?> model, List<? extends Field> fields) {
			super(null);
			len = fields.size();
			this.model = model;
			this.fields = fields;
			this.maker = model.maker();
		}

		public Class<T> getType() {
			return model.getType();
		}

		private Field getCurrentField() {
			if (currentField == null && pos < len) {
				currentField = fields.get(pos++);
			}
			return currentField;
		}

		public Object maker() {
			return maker;
		}

		@Override
		public T parse(Parser parser) {
			return (T) parser.parseList(this);
		}
		
		@Override
		public void parseItem(Object listMaker, Parser parser) {
			var c = getCurrentField();
			if (c != null) {
				currentField = null;
				c.parse(listMaker, parser);
			}
			else {
				parser.parse();
			}
		}
		
		@Override
		public T make(Object maker) {
			return (T) model.make(maker);
		}
	}
}