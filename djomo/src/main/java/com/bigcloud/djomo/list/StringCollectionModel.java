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
package com.bigcloud.djomo.list;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Supplier;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;

public class StringCollectionModel<T extends Collection<String>> extends CollectionModel<T> {

	public StringCollectionModel(Type type, ModelContext context, Supplier<T> constructor) {
		super(type, context, constructor, String.class);
	}

	@Override
	public void visitItems(T t, Visitor visitor) {
		if(t instanceof RandomAccess && t instanceof List l) {
			int len = t.size();
			for(int i = 0; i < len; i++) {
				visitor.visitListItem();
				String s = (String) l.get(i);
				if(s == null) {
					visitor.visitNull();
				}
				else {
					visitor.visitString(s);
				}
			}
		}
		else {
			for(var i: t) {
				visitor.visitListItem();
				if(i == null) {
					visitor.visitNull();
				}
				else {
					visitor.visitString(i);
				}
			}
		}
	}
	
	@Override
	public void parseItem(Object listMaker, Parser parser) {
		parser.parseListItem();
		var str = parser.parseString();
		addItem(listMaker, str == null ? null : str.toString());
	}
}