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
package com.bigcloud.djomo.base;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilterFactory;

/**
 * A parser implementation that reads from an instance of an existing object
 * structure instead of a serialized stream. Supports type conversion with
 * parser filters.
 * 
 * @author Alex Vigdor
 *
 */
public class InstanceParser extends BaseParser implements Parser {
	Object source;
	Model<?> sourceModel;

	public InstanceParser(Models context, Object source, ParserFilterFactory... filters) {
		super(context, filters);
		this.source = source;
	}

	@Override
	public Object parse() {
		if (source == null) {
			return null;
		}
		var osm = sourceModel;
		try {
			var m = sourceModel = models.get(source.getClass());
			if(m instanceof ObjectModel) {
				return parser.parseObject(models.mapModel);
			}
			if(m instanceof ListModel) {
				return parser.parseList(models.listModel);
			}
			return parser.parse(m);
		} finally {
			sourceModel = osm;
		}
	}
	
	@Override
	public Object parse(Model model) {
		if (source == null) {
			return null;
		}
		var osm = sourceModel;
		try {
			sourceModel = models.get(source.getClass());
			return model.parse(parser);
		} finally {
			sourceModel = osm;
		}
	}

	@Override
	public Object parseObject(ObjectModel model) {
		if (source == null) {
			return null;
		}
		var osm = sourceModel;
		try {
			var m = sourceModel = models.get(source.getClass());
			final Object maker = objectMaker(model);
			if (m instanceof ObjectModel som) {
				som.forEachField(source, (key, value) -> {
					var os = source;
					try {
						source = value;
						var field = parser.parseObjectField(model, key.toString());
						if (field != null) {
							field.parse(maker, parser);
						} else {
							parser.parse();
						}
					} finally {
						source = os;
					}
				});
			}
			return model.make(maker);
		} finally {
			sourceModel = osm;
		}
	}

	@Override
	public Field parseObjectField(ObjectModel model, CharSequence field) {
		return model.getField(field);
	}

	@Override
	public Object parseList(ListModel model) {
		if (source == null) {
			return null;
		}
		var osm = sourceModel;
		try {
			var m = sourceModel = models.get(source.getClass());
			Object maker = listMaker(model);
			if (m instanceof ListModel) {
				var os = source;
				try {
					((ListModel) sourceModel).forEachItem(source, item -> {
						source = item;
						model.parseItem(maker, parser);
					});
				} finally {
					source = os;
				}
			}
			return model.make(maker);
		} finally {
			sourceModel = osm;
		}
	}

	@Override
	public Object parseNull() {
		return null;
	}

	@Override
	public int parseInt() {
		if (source instanceof Number n) {
			return n.intValue();
		}
		return models.get(Integer.class).convert(source);
	}

	@Override
	public long parseLong() {
		if (source instanceof Number n) {
			return n.longValue();
		}
		return models.get(Long.class).convert(source);
	}

	@Override
	public float parseFloat() {
		if (source instanceof Number n) {
			return n.floatValue();
		}
		return models.get(Float.class).convert(source);
	}

	@Override
	public double parseDouble() {
		if (source instanceof Number n) {
			return n.doubleValue();
		}
		return models.get(Double.class).convert(source);
	}

	@Override
	public boolean parseBoolean() {
		if (source instanceof Boolean b) {
			return b.booleanValue();
		}
		return models.get(Boolean.class).convert(source);
	}

	@Override
	public String parseString() {
		return source == null ? null : source.toString();
	}

}
