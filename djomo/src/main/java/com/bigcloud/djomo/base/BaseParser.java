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

import java.util.function.Consumer;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ComplexModel;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Maker;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.filter.FilterParser;
/**
 * Baseline parser support, source agnostic
 * 
 * @author Alex Vigdor
 *
 */
public abstract class BaseParser implements Parser {
	protected final Models models;
	protected final Parser parser;
	
	public BaseParser(Models models, FilterParser... filters) {
		this.models = models;
		Parser end = this;
		if(filters!=null) {
			for(int i=0;i<filters.length;i++) {
				end = filters[i].parser(end);
			}
		}
		this.parser = end;
	}

	protected <T> T parseObjectModel(Model<T> model){
		ObjectModel odef;
		if (!(model instanceof ObjectModel)) {
			odef = models.mapModel;
		}
		else {
			odef = (ObjectModel) model;
		}
		var m = parser.parseObject(odef);
		return (T) m.make();
	}
	
	protected <T> T parseListModel(Model<T> model){
		ListModel ldef;
		if (!(model instanceof ListModel)) {
			ldef = models.listModel;
		}
		else {
			ldef = (ListModel) model;
		}
		var m = parser.parseList(ldef);
		return (T) m.make();
	}
	
	protected <T> T parseStringModel(Model<T> model) {
		SimpleModel sdef;
		if(!(model instanceof SimpleModel)) {
			sdef = models.stringModel;
		}
		else {
			sdef = (SimpleModel) model;
		}
		var t = parser.parseSimple(sdef);
		return (T) t;
	}
	
	protected <T> T parseNumberModel(Model<T> model) {
		SimpleModel sdef;
		if(!(model instanceof SimpleModel)) {
			sdef = models.numberModel;
		}
		else {
			sdef = (SimpleModel) model;
		}
		var t = parser.parseSimple(sdef);
		return (T) t;
	}

	protected <O, M extends Maker<O>> M maker(ComplexModel<O, M> definition){
		return definition.maker();
	}

	protected <V> V parseFieldValue(Field<?, ?, V> field) {
		return field.model().parse(parser);
	}

	@Override
	public <T> void parseListItem(Model<T> definition, Consumer<T> consumer) {
		consumer.accept(definition.parse(parser));
	}
	/**
	 * Entry point to allow filters to intercept top-level object visit; subclasses should extend visit as opposed to filter
	 * 
	 */
	public final <T> T filter(Model<T> model) {
		return model.parse(parser);
	}

	@Override
	public Models models() {
		return models;
	}
}
