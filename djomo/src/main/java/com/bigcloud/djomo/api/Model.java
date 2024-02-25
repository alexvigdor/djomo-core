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

import com.bigcloud.djomo.Models;

/**
 * A Model allows parsing, visiting and converting to a specific java type.
 * 
 * There are specialized sub-interfaces describing ObjectModel with named 
 * fields or ListModel with stream support, and SimpleModel which represent plain values like strings, numbers, and booleans.
 * 
 * @author Alex Vigdor
 *
 * @param <T> Class of object this model describes
 */
public interface Model<T> {
	/**
	 * 
	 * @return the java type this Model represents
	 */
	Class<T> getType();
	/**
	 * 
	 * @return the format this model parses from / visits to
	 */
	Format getFormat();
	/**
	 * Attempt to convert another object into an instance of the type represented by this Model
	 * @param o another object, such as a raw parsed string, list or map, to convert to this type
	 * @return an instance of this type based on the object passed in
	 */
	T convert(Object o);
	/**
	 * Materialize an instance of this type by pulling from the parser; should invoke parseList, parseObject or a primitive parse method
	 * @param parser The parser to used to pull an instance of this model, or of an other underlying Model this one relies upon
	 * @return an instance of this type based on date pulled from the parser
	 */
	T parse(Parser parser);
	/**
	 * Dispatch an instance of this type to a visitor; should invoke visitSimple, visitList or visitObject on the visitor with the instance, 
	 * or can invoke visit with an unwrapped object.
	 * @param obj
	 * @param visitor
	 */
	void visit(T obj, Visitor visitor);
	/**
	 * Retrieve the owning Models instance
	 * @return
	 */
	Models models();
}