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

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class AtomicBooleanModel extends BaseModel<AtomicBoolean> {
	final Model<Boolean> valueModel;

	public AtomicBooleanModel(Type type, ModelContext context) {
		super(type, context);
		valueModel = context.get(boolean.class);
	}

	@Override
	public AtomicBoolean convert(Object o) {
		if (o == null) {
			return null;
		}
		return new AtomicBoolean(valueModel.convert(o));
	}

	@Override
	public AtomicBoolean parse(Parser parser) {
		return new AtomicBoolean(parser.parseBoolean());
	}

	@Override
	public void visit(AtomicBoolean obj, Visitor visitor) {
		visitor.visitBoolean(obj.get());
	}

	@Override
	public Format getFormat() {
		return Format.BOOLEAN;
	}

}
