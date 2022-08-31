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

/**
 * A Maker is an abstraction over the intermediate state of an object as it is being assembled.
 * It might directly manipulate a bean, map or collection, or it might invoke a builder, or buffer up arguments to construct a Record.
 * 
 * @author Alex Vigdor
 *
 * @param <T>
 */
public interface Maker <T> {
	T make();
	Model<T> model();
}
