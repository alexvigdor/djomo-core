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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseModelFactory;

public class PolyModelFactory extends BaseModelFactory {

	@Override
	public Model<?> create(Type type, ModelContext context) {
		Class<?> rawType = getRawType(type);
		if (Optional.class.isAssignableFrom(rawType)) {
			return new OptionalModel<>(type, context);
		}
		if (OptionalInt.class.isAssignableFrom(rawType)) {
			return new OptionalIntModel(type, context);
		}
		if (OptionalLong.class.isAssignableFrom(rawType)) {
			return new OptionalLongModel(type, context);
		}
		if (OptionalDouble.class.isAssignableFrom(rawType)) {
			return new OptionalDoubleModel(type, context);
		}
		if(AtomicInteger.class.isAssignableFrom(rawType)) {
			return new AtomicIntegerModel(type, context);
		}
		if(AtomicLong.class.isAssignableFrom(rawType)) {
			return new AtomicLongModel(type, context);
		}
		if(AtomicBoolean.class.isAssignableFrom(rawType)) {
			return new AtomicBooleanModel(type, context);
		}
		if(AtomicReference.class.isAssignableFrom(rawType)) {
			return new AtomicReferenceModel<>(type, context);
		}
		if(Future.class.isAssignableFrom(rawType)) {
			return new FutureModel<>(type, context);
		}
		if(Supplier.class.isAssignableFrom(rawType)) {
			return new SupplierModel<>(type, context);
		}
		if(Object.class == type) {
			return new AnyModel(type, context);
		}
		return null;
	}

}
