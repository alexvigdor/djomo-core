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
package com.bigcloud.djomo.filter.visitors;

import java.util.Objects;
import java.util.stream.Stream;

import com.bigcloud.djomo.ModelType;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.base.BaseVisitorFilter;

/**
 * Prevent null field values or list items from being visited
 * 
 * @author Alex Vigdor
 *
 */
public class OmitNullItemVisitor extends BaseVisitorFilter {
	
	@Override
	public <T> void visitList(T model, ListModel<T> definition) {
		var streamModel = ModelType.of(Stream.class, definition.itemModel().getType());
		visitor.visitList(definition.stream(model).filter(Objects::nonNull), models().get(streamModel));
	}

}
