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
import com.bigcloud.djomo.api.VisitorFilterFactory;
import com.bigcloud.djomo.io.CharSink;

public class IndentingJsonWriter extends BaseJsonWriter {
	private final char[] indent;
	private int depth = 0;

	public IndentingJsonWriter(Models context, CharSink sink, String indent, VisitorFilterFactory... filters) {
		super(context, sink, filters);
		this.indent = indent.toCharArray();
	}
	protected void indent() {
		for(int i=0;i<depth;i++) {
			System.arraycopy(indent, 0, buffer, pos, indent.length);
			pos+=indent.length;
		}
	}

	public <T> void visitObject(T model, ObjectModel<T> definition) {
		char[] buf = buffer;
		if(pos==BUF_LEN) {
			sink.next(BUF_LEN);
			pos = 0;
		}
		buf[pos++] = '{';
		first = true;
		depth++;
		super.visitObject(model, definition);
		depth--;
		if(!first) {
			reserve(2+(depth*indent.length));
			buf[pos++] = '\n';
			indent();
		}
		else {
			first = false;
			if (pos == BUF_LEN) {
				sink.next(BUF_LEN);
				pos = 0;
			}
		}
		buf[pos++] = '}';
	}

	public <T> void visitList(T model, ListModel<T> definition) {
		char[] buf = buffer;
		if(pos==BUF_LEN) {
			sink.next(BUF_LEN);
			pos = 0;
		}
		buf[pos++] = '[';
		first = true;
		depth++;
		super.visitList(model, definition);
		depth--;
		if(!first) {
			reserve(2+(depth*indent.length));
			buf[pos++] = '\n';
			indent();
		}
		else {
			first = false;
			if (pos == BUF_LEN) {
				sink.next(BUF_LEN);
				pos = 0;
			}
		}
		buf[pos++] = ']';
	}

	public void visitObjectField(Object name) {
		char[] buf = buffer;
		if (!first) {
			reserve(2+(depth*indent.length));
			buf[pos++] = ',';
		}
		else {
			reserve(1+(depth*indent.length));
			first = false;
		}
		buf[pos++] = '\n';
		indent();
		visitString(name.toString());
		reserve(3);
		buf[pos++] = ' ';
		buf[pos++] = ':';
		buf[pos++] = ' ';
	}

	public void visitListItem() {
		char[] buf = buffer;
		if (!first) {
			reserve(2+(depth*indent.length));
			buf[pos++] = ',';
		}
		else {
			reserve(1+(depth*indent.length));
			first = false;
		}
		buf[pos++] = '\n';
		indent();
	}
}
