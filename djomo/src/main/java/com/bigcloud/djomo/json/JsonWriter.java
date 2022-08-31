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
package com.bigcloud.djomo.json;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.filter.FilterVisitor;
import com.bigcloud.djomo.io.CharSink;

/**
 * A ModelVisitor that produces a serialized JSON representation of a Model
 * 
 * @author Alex Vigdor
 *
 */
public class JsonWriter extends BaseJsonWriter implements Printer, AutoCloseable {

	public JsonWriter(Models context, CharSink sink, FilterVisitor... filters) {
		super(context, sink, filters);
	}

	public <T> void visitObject(T model, ObjectModel<T, ?, ?, ?, ?> definition) {
		char[] buf = buffer;
		int p;
		if ((p = pos) == BUF_LEN) {
			sink.next(BUF_LEN);
			p = 0;
		}
		buf[p] = '{';
		pos = p + 1;
		first = true;
		definition.forEachField(model, current::visitObjectField);
		if ((p = pos) == BUF_LEN) {
			sink.next(BUF_LEN);
			p = 0;
		}
		buf[p] = '}';
		pos = p + 1;
		first = false;
	}

	public <T> void visitList(T model, ListModel<T, ?, ?> definition) {
		char[] buf = buffer;
		int p;
		if ((p = pos) == BUF_LEN) {
			sink.next(BUF_LEN);
			p = 0;
		}
		buf[p] = '[';
		pos = p + 1;
		first = true;
		definition.forEachItem(model, current::visitListItem);
		if ((p = pos) == BUF_LEN) {
			sink.next(BUF_LEN);
			p = 0;
		}
		buf[p] = ']';
		pos = p + 1;
		first = false;
	}

	public void visitObjectField(Object name, Object value) {
		char[] buf = buffer;
		int p;
		if (!first) {
			if ((p = pos) == BUF_LEN) {
				sink.next(BUF_LEN);
				p = 0;
			}
			buf[p] = ',';
			pos = p + 1;
		} else {
			first = false;
		}
		quote(name.toString());
		if ((p = pos) == BUF_LEN) {
			sink.next(BUF_LEN);
			p = 0;
		}
		buf[p] = ':';
		pos = p + 1;
		current.visit(value);
	}

	public void visitListItem(Object value) {
		if (!first) {
			int p;
			if ((p = pos) == BUF_LEN) {
				sink.next(BUF_LEN);
				p = 0;
			}
			buffer[p] = ',';
			pos = p + 1;
		} else {
			first = false;
		}
		current.visit(value);
	}

}
