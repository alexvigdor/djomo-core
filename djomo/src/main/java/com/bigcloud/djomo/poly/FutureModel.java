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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;

public class FutureModel<V> extends BaseModel<Future<V>> {
	final Model<V> valueModel;

	public FutureModel(Type type, ModelContext context) {
		super(type, context);
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			valueModel = (Model<V>) context.get(pt.getActualTypeArguments()[0]);
		} else {
			valueModel = (Model<V>) context.get(Object.class);
		}
	}

	@Override
	public Future<V> convert(Object o) {
		if (valueModel != null) {
			o = valueModel.convert(o);
		}
		CompletableFuture<V> future = new CompletableFuture<>();
		future.complete((V) o);
		return future;
	}

	@Override
	public Future<V> parse(Parser parser) {
		CompletableFuture<V> future = new CompletableFuture<>();
		future.complete(valueModel.parse(parser));
		return future;
	}

	@Override
	public void visit(Future<V> obj, Visitor visitor) {
		V value;
		try {
			value = obj.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Error resolving future", e);
		}
		valueModel.tryVisit(value, visitor);
	}

}
