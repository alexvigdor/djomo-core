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
		var ind = indent;
		int il = ind.length;
		int d = depth;
		var buf = buffer;
		int p = reserve(1 + d * il);
		buf[p++] = '\n';
		for (int i = 0; i < d; i++) {
			System.arraycopy(ind, 0, buf, p, il);
			p += il;
		}
		pos = p;
	}

	public <T> void visitObject(T model, ObjectModel<T> definition) {
		append('{');
		first = true;
		depth++;
		super.visitObject(model, definition);
		depth--;
		if (!first) {
			indent();
		} else {
			first = false;
		}
		append('}');
	}

	public <T> void visitList(T model, ListModel<T> definition) {
		append('[');
		first = true;
		depth++;
		super.visitList(model, definition);
		depth--;
		if (!first) {
			indent();
		} else {
			first = false;
		}
		append(']');
	}

	public void visitObjectField(Object name) {
		delimit();
		visitString(name.toString());
		append(' ', ':', ' ');
	}

	public void visitListItem() {
		delimit();
	}

	private void delimit() {
		if (!first) {
			append(',');
		} else {
			first = false;
		}
		indent();
	}
}
