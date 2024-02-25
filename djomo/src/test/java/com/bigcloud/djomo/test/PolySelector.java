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
package com.bigcloud.djomo.test;

import java.lang.reflect.Type;
import java.util.Map;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;

public class PolySelector extends Resolver<Poly> {
	Model<PolyBar> polyBar;
	Model<PolyFoo> polyFoo;

	@Override
	public Poly resolve(Parser parser) {
		Models models = parser.models();
		Map data = (Map) parser.parseObject(models.mapModel);
		if (data.containsKey("bar")) {
			return polyBar.convert(data);
		}
		return polyFoo.convert(data);
	}

	@Override
	public void init(ModelContext models, Type[] typeArgs) {
		polyBar = models.get(PolyBar.class);
		polyFoo = models.get(PolyFoo.class);
	}

	@Override
	public Format getFormat() {
		return Format.OBJECT;
	}
}