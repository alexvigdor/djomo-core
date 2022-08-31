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
package com.bigcloud.djomo.simple;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.UUID;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.base.BaseModelFactory;

public class SimpleModelFactory extends BaseModelFactory {
	private final MethodHandles.Lookup lookup = MethodHandles.lookup();

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Model<?> create(Type type, ModelContext context){
		Class rawType = getRawType(type);
		if(Enum.class.isAssignableFrom(rawType)) {
			return new EnumModel(type, context);
		}
		else if(Number.class.isAssignableFrom(rawType)) {
			if(rawType == Long.class) {
				return new LongModel(type, context);
			}
			if(rawType == Integer.class) {
				return new IntegerModel(type, context);
			}
			if(rawType == Float.class) {
				return new FloatModel(type, context);
			}
			if(rawType == Double.class) {
				return new DoubleModel(type, context);
			}
			if(rawType == Byte.class) {
				return new ByteModel(type, context);
			}
			if(rawType == Short.class) {
				return new ShortModel(type, context);
			}
			if(rawType == Number.class) {
				return new NumberModel(type, context);
			}
			if(rawType == BigDecimal.class) {
				return new BigDecimalModel(type, context);
			}
			if(rawType == BigInteger.class) {
				return new BigIntegerModel(type, context);
			}
		}
		else if(Boolean.class.isAssignableFrom(rawType)) {
			return new BooleanModel(type, context);
		}
		else if(Character.class.isAssignableFrom(rawType)) {
			return new CharModel(type, context);
		}
		else if(rawType.isPrimitive()) {
			if(rawType == long.class) {
				return new LongModel(type, context);
			}
			if(rawType == int.class) {
				return new IntegerModel(type, context);
			}
			if(rawType == float.class) {
				return new FloatModel(type, context);
			}
			if(rawType == double.class) {
				return new DoubleModel(type, context);
			}
			if(rawType == boolean.class) {
				return new BooleanModel(type, context);
			}
			if(rawType == byte.class) {
				return new ByteModel(type, context);
			}
			if(rawType == short.class) {
				return new ShortModel(type, context);
			}
			if(rawType == char.class) {
				return new CharModel(type, context);
			}
			throw new RuntimeException("Unknown primitive type " + rawType.getName());
		}
		else if (ZoneOffset.class.isAssignableFrom(rawType)) {
			return magicString(ZoneOffset.class, context, "of",  String.class);
		}
		else if(ZoneId.class.isAssignableFrom(rawType)) {
			return magicString(ZoneId.class, context, "of",  String.class);
		}
		else if((TemporalAmount.class.isAssignableFrom(rawType) || TemporalAccessor.class.isAssignableFrom(rawType)) && rawType.getPackageName().equals("java.time")) {
			return magicString(rawType, context, "parse",  CharSequence.class);
		}
		else if(String.class == rawType) {
			return new StringModel(context);
		}
		else if(URI.class == rawType) {
			return magicString(URI.class, context, "create",  String.class);
		}
		else if(URL.class == rawType) {
			return magicString(URL.class, context, null,  String.class);
		}
		else if(Class.class == rawType) {
			return magicString(Class.class, context, "forName",  String.class, "getName");
		}
		else if(UUID.class == rawType) {
			return new UUIDModel(rawType, context);
		}
		else if(Throwable.class.isAssignableFrom(rawType)) {
			return magicString(Throwable.class, context, null, String.class);
		}
	
		return null;
	}

	private <T> Model<T> magicString(Class<?> rawType, ModelContext context, String constructorMethod, Class<?> param) {
		return magicString(rawType, context, constructorMethod, param, "toString");
	}
	private <T> Model<T> magicString(Class<?> rawType, ModelContext context, String constructorMethod, Class<?> param, String toStringMethod) {
		try {
			MethodHandle constructor;
			if(constructorMethod == null) {
				constructor = lookup.unreflectConstructor(rawType.getDeclaredConstructor(param));
			}
			else {
				constructor = lookup.unreflect(rawType.getDeclaredMethod(constructorMethod, param));
			}
			return new StringBasedModel<T>(rawType, context, constructor, lookup.unreflect(rawType.getDeclaredMethod(toStringMethod)));
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
			throw new RuntimeException("Unable to find '" + constructorMethod + "' method on " + rawType.getName(), e);
		}
	}
}
