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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bigcloud.djomo.Models;
/**
 * Interface for object parsers; the interface is source agnostic, it could be parsing from a binary or character stream, or from an already existing object model.
 * 
 * @author Alex Vigdor
 *
 */
public interface Parser {
	<T> T parse(Model<T> model);
	<O, M extends ObjectMaker<O, F, V>, F extends Field<O,?,V>, V> M parseObject(ObjectModel<O, M, F, ?, V> model);
	<O, M extends ObjectMaker<O, F, V>, F extends Field<O,?,V>, V> void parseObjectField(ObjectModel<O, M, F, ?, V> model, CharSequence field, BiConsumer<F, V> consumer);
	<L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> model);
	<T> void parseListItem(Model<T> model, Consumer<T> consumer);
	<T> T parseSimple(SimpleModel<T> model);
	Object parseNull();
	Models models();
}
