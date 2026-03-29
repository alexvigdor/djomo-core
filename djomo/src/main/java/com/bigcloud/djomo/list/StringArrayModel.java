/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseListModel;

public class StringArrayModel extends BaseListModel<String[]>{

	public StringArrayModel(ModelContext context) {
		super(String[].class, context, context.get(String.class));
	}

	@Override
	public void forEachItem(String[] t, Consumer consumer) {
		for(String l: t) {
			consumer.accept(l);
		}
	}
	
	@Override
	public void parseItem(Object listMaker, Parser parser) {
		parser.parseListItem();
		var str = parser.parseString();
		addItem(listMaker, str == null ? null : str.toString());
	}

	@Override
	public void visitItems(String[] t, Visitor visitor) {
		for(String l: t) {
			visitor.visitListItem();
			if(l == null) {
				visitor.visitNull();
			}
			else {
				visitor.visitString(l);
			}
		}
	}

	@Override
	public Stream stream(String[] t) {
		return Arrays.stream(t);
	}

	@Override
	public Object maker(String[] obj) {
		int len = obj.length;
		var buf = new StringArrayBuffer();
		buf.buffer = len == 0 ? new String[8] : Arrays.copyOf(obj, len * 2);
		buf.pointer = len;
		return buf;
	}

	@Override
	public Object maker() {
		var buf = new StringArrayBuffer();
		buf.buffer = new String[8];
		return buf;
	}

	@Override
	public String[] make(Object maker) {
		var buf = (StringArrayBuffer) maker;
		return Arrays.copyOf(buf.buffer, buf.pointer);
	}

	@Override
	protected final void addItem(Object maker, Object item) {
		var buf = (StringArrayBuffer) maker;
		var p = buf.pointer;
		if(p == buf.buffer.length) {
			buf.buffer = Arrays.copyOf(buf.buffer, p * 2);
		}
		buf.buffer[p] = (String) item;
		buf.pointer = p + 1;
	}

	private static class StringArrayBuffer{
		private String[] buffer;
		private int pointer;
	}
}
