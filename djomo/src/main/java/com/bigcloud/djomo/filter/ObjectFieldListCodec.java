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
package com.bigcloud.djomo.filter;

import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.filter.parsers.ObjectFieldListParser;
import com.bigcloud.djomo.filter.visitors.ObjectFieldListVisitor;

/**
 * Convert an object with known fields into a list of field values without
 * names; allows more compact serialization but requires applying codec on both
 * read and write
 * 
 * @author Alex Vigdor
 *
 */
@Visit(ObjectFieldListVisitor.class)
@Parse(ObjectFieldListParser.class)
public class ObjectFieldListCodec {


}
