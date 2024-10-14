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
package com.bigcloud.djomo.api;

import java.lang.reflect.Type;

import com.bigcloud.djomo.Models;
/**
 * <p>
 * Extension point for provisioning custom {@link Model} instances; must be added to a {@link Models.Builder}s to take effect.  
 * </p>
 * <p>
 * Given a type, a ModelFactory should only return a Model if it recognizes that type;
 * the ModelFactory should return null for an unrecognized or unsupported type, to allow the next factory in the chain to process it.
 * </p>
 * 
 * @author Alex Vigdor
 *
 */
@FunctionalInterface
public interface ModelFactory {
	Model<?> create(Type type, ModelContext context);
}
