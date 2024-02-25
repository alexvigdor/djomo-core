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
package com.bigcloud.djomo.rs.test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.filter.visitors.InjectVisitor;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SampleResolver implements ContextResolver<Json> {
	final Json norm;
	final Json sensitive;
	
	public SampleResolver() {
		Models models = new Models();
		norm = new Json(models);
		sensitive = Json.builder()
				.models(models)
				.visit(new InjectVisitor<Sensitive>("classification") {
					@Override
					public Object value(Sensitive obj) {
						return obj.message().length()+" secret characters";
					}
				})
				.build();
	}

	@Override
	public Json getContext(Class<?> type) {
		if(type.equals(Sensitive.class)) {
			return sensitive;
		}
		return norm;
	}

}
