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

/**
 * A ModelVisitor that produces a serialized JSON representation of a Model
 * 
 * @author Alex Vigdor
 *
 */
public class JsonWriter extends BaseJsonWriter implements AutoCloseable {

	public JsonWriter(Models context, CharSink sink, VisitorFilterFactory... filters) {
		super(context, sink, filters);
	}

	@Override
	public <T> void visitObject(T model, ObjectModel<T> definition) {
		append('{');
		first = true;
		definition.visitFields(model, current);
		append('}');
		first = false;
	}

	@Override
	public <T> void visitList(T model, ListModel<T> definition) {
		append('[');
		first = true;
		definition.visitItems(model, current);
		append(']');
		first = false;
	}

	@Override
	public void visitObjectField(Object name) {
		if(name instanceof SafeCharSequence scs) {
			int len = scs.length();
			var lpos = reserve(len + 4);
			var buf = buffer;
			if(first) {
				first = false;
			}
			else {
				buf[lpos++] = ',';
			}
			buf[lpos++] = '"';
			lpos = scs.getChars(buf, lpos);
			buf[lpos++]= '"';
			buf[lpos++]=':';
			pos = lpos;
			return;
		}
		int p;
		char[] buf = buffer;
		if (!first) {
			p = reserve(1);
			buf[p] = ',';
			pos = p + 1;
		} else {
			first = false;
		}
		if(name instanceof CharSequence cs) {
			visitString(cs);
		}
		else {
			visitString(name.toString());
		}
		p = reserve(1);
		buf[p] = ':';
		pos = p + 1;
	}

	@Override
	public void visitListItem() {
		if (!first) {
			append(',');
		} else {
			first = false;
		}
	}

}
