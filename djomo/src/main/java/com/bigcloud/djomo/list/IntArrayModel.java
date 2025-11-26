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
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseListModel;

public class IntArrayModel extends BaseListModel<int[]>{

	public IntArrayModel(ModelContext context) {
		super(int[].class, context, context.get(int.class));
	}

	@Override
	public void forEachItem(int[] t, Consumer consumer) {
		for(int l: t) {
			consumer.accept(l);
		}
	}

	@Override
	public void visitItems(int[] t, Visitor visitor) {
		for(int l: t) {
			visitor.visitListItem();
			visitor.visitInt(l);
		}
	}

	@Override
	public Stream stream(int[] t) {
		return Arrays.stream(t).boxed();
	}

	@Override
	public Object maker(int[] obj) {
		int len = obj.length;
		var buf = new IntArrayBuffer();
		buf.buffer = len == 0 ? new int[8] : Arrays.copyOf(obj, len * 2);
		buf.pointer = len;
		return buf;
	}

	@Override
	public Object maker() {
		var buf = new IntArrayBuffer();
		buf.buffer = new int[8];
		return buf;
	}

	@Override
	public int[] make(Object maker) {
		var buf = (IntArrayBuffer) maker;
		return Arrays.copyOf(buf.buffer, buf.pointer);
	}

	@Override
	protected void addItem(Object maker, Object item) {
		var buf = (IntArrayBuffer) maker;
		var p = buf.pointer;
		if(p == buf.buffer.length) {
			buf.buffer = Arrays.copyOf(buf.buffer, p * 2);
		}
		buf.buffer[p] = (int) item;
		buf.pointer = p + 1;
	}

	private static class IntArrayBuffer{
		private int[] buffer;
		private int pointer;
	}
}
