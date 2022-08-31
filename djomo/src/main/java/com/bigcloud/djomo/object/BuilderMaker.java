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
package com.bigcloud.djomo.object;

import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.base.BaseMaker;

public class BuilderMaker<T> extends BaseMaker<T, BuilderModel<T>> implements ObjectMaker<T, BeanField<T, Object>, Object>  {
	final Object builder;

	public BuilderMaker(BuilderModel<T> model) {
		super(model);
		builder = model.builder();
	}

	@Override
	public T make() {
		return model.build(builder);
	}

	@Override
	public void field(BeanField<T, Object> field, Object value) {
		field.set(builder, value);
	}

}
