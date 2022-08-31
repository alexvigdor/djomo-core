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

import java.util.List;
import java.util.function.Consumer;

import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;

/**
 * Convert an object with known fields into a list of field values without
 * names; allows more compact serialization but requires applying codec on both
 * read and write
 * 
 * @author Alex Vigdor
 *
 */
@Visit(ObjectFieldListCodec.Visitor.class)
@Parse(ObjectFieldListCodec.Parser.class)
public class ObjectFieldListCodec {
	public static class Visitor extends FilterVisitor {
		public <T> void visitObject(T obj, ObjectModel<T, ?, ?, ?, ?> model) {
			List<Field> fields = (List<Field>) model.fields();
			if (fields != null) {
				visitList(obj, new FilterListModel<>(null) {
					public void forEachItem(T obj, Consumer<Object> consumer) {
						for (Field f : fields) {
							consumer.accept(f.get(obj));
						}
					}
				});
			} else {
				super.visitObject(obj, model);
			}
		}
	}

	public static class Parser extends FilterParser {
		public <T> T parse(Model<T> model) {
			if (model instanceof ObjectModel<T, ?, ?, ?, ?> om) {
				List<Field> fields = (List<Field>) om.fields();
				if (fields != null) {
					return parser.parse(new FilterListModel<T, ListMaker<T, Object>, Object>(null) {
						final int len = fields.size();
						int pos = 0;
						Field currentField;

						public Class<T> getType() {
							return model.getType();
						}

						public ListMaker<T, Object> maker() {
							ObjectMaker maker = om.maker();
							Model<T> m = this;
							return new ListMaker<T, Object>() {

								@Override
								public T make() {
									return (T) maker.make();
								}

								@Override
								public Model<T> model() {
									return m;
								}

								@Override
								public void item(Object arg0) {
									if (currentField != null) {
										maker.field(currentField, arg0);
									}
								}
							};
						}

						@Override
						public Model<Object> itemModel() {
							return new FilterModel<>(null) {
								public Object parse(com.bigcloud.djomo.api.Parser parser) {
									if (pos < len) {
										var c = fields.get(pos++);
										currentField = c;
										return c.model().parse(parser);
									}
									currentField = null;
									return parser.models().anyModel.parse(parser);
								}
							};
						}
					});
				}
			}
			return parser.parse(model);
		}
	}
}
