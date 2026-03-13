/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import com.bigcloud.djomo.annotation.Remodel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseModelFactory;
import com.bigcloud.djomo.error.AnnotationException;

public class RemodelFactory extends BaseModelFactory {

	@Override
	public Model<?> create(Type type, ModelContext context) {
		Class<?> rawType = getRawType(type);
		var remodel = rawType.getAnnotation(Remodel.class);
		if (remodel != null) {
			var modelClass = remodel.value();
			try {
				var constructor = modelClass.getConstructor(Type.class, ModelContext.class);
				return (Model<?>) constructor.newInstance(type, context);
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new AnnotationException(
						"Models use with the Remodel annotation must present a two-argument constructor accepting Type and ModelContext.  Custom models should inherit from BaseModel and invoke its matching constructor.",
						e);
			}
		}
		return null;
	}

}
