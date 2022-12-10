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
package com.bigcloud.djomo.rs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.bigcloud.djomo.Json;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;
/**
 * Base class for djomo entity providers, supports lookup and caching of Json objects by type from a user-provided ContextResolver&lt;Json&gt;.
 * 
 * @author Alex Vigdoor
 *
 */
public class JsonContext {
	private static final AtomicReference<Json> defaultJson = new AtomicReference<>();
	protected Providers providers;
	private final Map<Class<?>, Json> jsons = new ConcurrentHashMap<>();

	@Context
	public void setProviders(Providers providers) {
		this.providers = providers;
	}

	public Json getJson(Class<?> type) {
		Json json = jsons.get(type);
		if (json != null) {
			return json;
		}
		return jsons.computeIfAbsent(type, t -> {
			if (providers != null) {
				ContextResolver<Json> resolver = providers.getContextResolver(Json.class,
						MediaType.APPLICATION_JSON_TYPE);
				if (resolver != null) {
					return resolver.getContext(t);
				}
			}
			Json j = defaultJson.get();
			if (j == null) {
				j = defaultJson.updateAndGet(x -> {
					if (x != null) {
						return x;
					}
					return new Json();
				});
			}
			return j;
		});
	}
}
