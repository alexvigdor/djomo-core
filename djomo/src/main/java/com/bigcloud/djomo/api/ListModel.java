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

import java.util.function.Consumer;
import java.util.stream.Stream;
/**
 * A ListModel allows iterating the items in a list and producing a stream of the items in the list.
 * 
 * @author Alex Vigdor
 *
 * @param <T> Class of object this model describes
 */
public interface ListModel<T> extends Model<T> {
	void forEachItem(T t, Consumer consumer);
	void visitItems(T t, Visitor visitor);
	void parseItem(Object listMaker, Parser parser);
	Stream stream(T t);
	Model itemModel();
	Object maker(T obj);
	Object maker();
	T make(Object maker);
}
