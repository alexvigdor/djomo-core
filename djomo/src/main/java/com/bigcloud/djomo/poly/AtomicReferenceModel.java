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
package com.bigcloud.djomo.poly;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class AtomicReferenceModel<V> extends BaseModel<AtomicReference<V>> {
	final Model<V> valueModel;

	public AtomicReferenceModel(Type type, ModelContext context) {
		super(type, context);
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			valueModel = (Model<V>) context.get(pt.getActualTypeArguments()[0]);
		} else {
			valueModel = (Model<V>) context.get(Object.class);
		}
	}

	@Override
	public AtomicReference<V> convert(Object o) {
		return new AtomicReference<>(valueModel.convert(o));
	}

	@Override
	public AtomicReference<V> parse(Parser parser) {
		return new AtomicReference<>(valueModel.parse(parser));
	}

	@Override
	public void visit(AtomicReference<V> obj, Visitor visitor) {
		visitor.visit(obj.get());
	}

	@Override
	public Format getFormat() {
		return valueModel.getFormat();
	}

}
