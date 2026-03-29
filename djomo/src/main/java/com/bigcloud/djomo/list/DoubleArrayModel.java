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

public class DoubleArrayModel extends BaseListModel<double[]>{

	public DoubleArrayModel(ModelContext context) {
		super(double[].class, context, context.get(double.class));
	}

	@Override
	public void forEachItem(double[] t, Consumer consumer) {
		for(double l: t) {
			consumer.accept(l);
		}
	}

	@Override
	public void visitItems(double[] t, Visitor visitor) {
		for(double l: t) {
			visitor.visitListItem();
			visitor.visitDouble(l);
		}
	}

	@Override
	public Stream stream(double[] t) {
		return Arrays.stream(t).boxed();
	}

	@Override
	public Object maker(double[] obj) {
		int len = obj.length;
		var buf = new DoubleArrayBuffer();
		buf.buffer = len == 0 ? new double[8] : Arrays.copyOf(obj, len * 2);
		buf.pointer = len;
		return buf;
	}

	@Override
	public Object maker() {
		var buf = new DoubleArrayBuffer();
		buf.buffer = new double[8];
		return buf;
	}

	@Override
	public double[] make(Object maker) {
		var buf = (DoubleArrayBuffer) maker;
		return Arrays.copyOf(buf.buffer, buf.pointer);
	}

	@Override
	public void parseItem(Object maker, Parser parser) {
		parser.parseListItem();
		addDouble((DoubleArrayBuffer) maker, parser.parseDouble());
	}

	@Override
	protected final void addItem(Object maker, Object item) {
		addDouble((DoubleArrayBuffer) maker, (double) item);
	}

	private void addDouble(DoubleArrayBuffer buffer, double value) {
		var p = buffer.pointer;
		var buf = buffer.buffer;
		if(p == buf.length) {
			buf = buffer.buffer = Arrays.copyOf(buf, p * 2);
		}
		buf[p] = value;
		buffer.pointer = p + 1;
	}

	private static class DoubleArrayBuffer{
		private double[] buffer;
		private int pointer;
	}
}
