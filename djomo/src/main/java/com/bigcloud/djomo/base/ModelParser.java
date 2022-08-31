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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.filter.FilterParser;
import com.bigcloud.djomo.poly.AnyModel;
/**
 * A parser implementation that reads from an existing object structure instead of a serialized stream. Supports type conversion with parser filters.
 * 
 * @author Alex Vigdor
 *
 */
public class ModelParser extends BaseParser implements Parser{
	Object source;
	Model<?> sourceModel;
	
	public ModelParser(Models context, Object source, FilterParser... filters) {
		super(context, filters);
		this.source = source;
	}

	@Override
	public <T> T parse(Model<T> model) {
		if(source == null) {
			return null;
		}
		var osm = sourceModel;
		try {
			var m = sourceModel = models.get(source.getClass());
			if(m instanceof ObjectModel) {
				return parseObjectModel(model);
			}
			if(m instanceof ListModel) {
				return parseListModel(model);
			}
			if(m instanceof SimpleModel) {
				if(model instanceof AnyModel) {
					return parseStringModel((SimpleModel<T>) m);
				}
				return parseStringModel(model);
			}
			return null;
		}
		finally {
			sourceModel = osm;
		}
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> M parseObject(ObjectModel<O, M, F,?,V> model) {
		final M maker = maker(model);
		final BiConsumer<F, V> consumer = maker::field;
		if(sourceModel instanceof ObjectModel) {
			((ObjectModel<Object,?,?, ?, ?>)sourceModel).forEachField(source, (key, value)->{
				var os = source;
				try {
					source =value;
					parser.parseObjectField(model, key.toString(), consumer);
				}
				finally {
					source = os;
				}
			});
		}
		return maker;
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F,?,V> model, String field, BiConsumer<F, V> consumer) {
		F mf = model.getField(field);
		if(mf!=null) {
			var v = parseFieldValue(mf);
			consumer.accept(mf, v);
		}
		else {
			parser.parse(models.anyModel);
		}
	}

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> model) {
		M maker = maker(model);
		Model<I> valdef = model.itemModel();
		Consumer<I> it = maker::item;
		if(sourceModel instanceof ListModel) {
			var os = source;
			try {
				((ListModel)sourceModel).forEachItem(source, item->{
					source = item;
					parser.parseListItem(valdef, it);
				});
			}
			finally {
				source = os;
			}
		}
		return maker;
	}

	@Override
	public <T> T parseSimple(SimpleModel<T> model) {
		return model.convert(source);
	}

	@Override
	public Object parseNull() {
		return null;
	}

}
