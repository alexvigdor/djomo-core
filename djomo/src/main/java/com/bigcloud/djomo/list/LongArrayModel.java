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

public class LongArrayModel extends BaseListModel<long[]>{

	public LongArrayModel(ModelContext context) {
		super(long[].class, context, context.get(long.class));
	}

	@Override
	public void forEachItem(long[] t, Consumer consumer) {
		for(long l: t) {
			consumer.accept(l);
		}
	}

	@Override
	public void visitItems(long[] t, Visitor visitor) {
		for(long l: t) {
			visitor.visitListItem();
			visitor.visitLong(l);
		}
	}

	@Override
	public Stream stream(long[] t) {
		return Arrays.stream(t).boxed();
	}

	@Override
	public Object maker(long[] obj) {
		int len = obj.length;
		var buf = new LongArrayBuffer();
		buf.buffer = len == 0 ? new long[8] : Arrays.copyOf(obj, len * 2);
		buf.pointer = len;
		return buf;
	}

	@Override
	public Object maker() {
		var buf = new LongArrayBuffer();
		buf.buffer = new long[8];
		return buf;
	}

	@Override
	public long[] make(Object maker) {
		var buf = (LongArrayBuffer) maker;
		return Arrays.copyOf(buf.buffer, buf.pointer);
	}

	@Override
	protected void addItem(Object maker, Object item) {
		var buf = (LongArrayBuffer) maker;
		var p = buf.pointer;
		if(p == buf.buffer.length) {
			buf.buffer = Arrays.copyOf(buf.buffer, p * 2);
		}
		buf.buffer[p] = (long) item;
		buf.pointer = p + 1;
	}

	private static class LongArrayBuffer{
		private long[] buffer;
		private int pointer;
	}
}
