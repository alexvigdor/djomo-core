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

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.SimpleModel;
import com.bigcloud.djomo.base.BaseParser;
import com.bigcloud.djomo.error.ModelException;
import com.bigcloud.djomo.error.UnexpectedPrimitiveException;
import com.bigcloud.djomo.filter.FilterParser;
import com.bigcloud.djomo.io.Buffer;
import com.bigcloud.djomo.simple.StringBasedModel;
import com.bigcloud.djomo.simple.StringModel;

public class JsonParser extends BaseParser implements Parser {
	final Buffer input;
	final Buffer overflow;

	public JsonParser(Models context, Buffer input, Buffer overflow, FilterParser... filters) {
		super(context, filters);
		this.input = input;
		this.overflow = overflow;
	}

	@Override
	public <T> T parse(Model<T> definition) {
		try {
			final var buf = this.input;
			final var t = this.parser;
			while(true) {
				switch (buf.read()) {
					case -1:
						throw new ModelException("Model incomplete at "+buf.describe());
					case '{':
						return parseObjectModel(definition);
					case '[':
						return parseListModel(definition);
					case '"':
						boolean fixQuote = !(definition instanceof StringBasedModel) && !(definition instanceof StringModel);
						T o = (T) parseStringModel(definition);
						if(fixQuote && buf.read() != '"') {
							buf.unread();
						}
						return o;
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
						break;
					case 't':
					case 'f':
						buf.unread();
						return (T) t.parseSimple(models.booleanModel);
					case 'n':
						buf.unread();
						return (T) t.parseNull();
					default:
						buf.unread();
						return parseNumberModel(definition);
				}
			}
		}
		catch(IOException e) {
			throw new ModelException("Error parsing JSON", e);
		}
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O,?,V>, V> M parseObject(ObjectModel<O, M, F, ?, V> model) {
		final M maker = maker(model);
		final BiConsumer<F, V> consumer = maker::field;
		final var buf = this.input;
		final var t = this.parser;
		final var o = this.overflow;
		final var s = models.stringModel;
		try {
			while (true) {
				switch (buf.read()) {
					case -1:
						throw new ModelException("Model incomplete at "+buf.describe());
					case '"':
						t.parseObjectField(model, s.parse(buf, o), consumer);
						break;
					case '}':
						return maker;
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
					case ',' :
						break;
					default:
						throw new ModelException("Unexpected character at "+buf.describe());
				}
			}
		}
		catch(IOException e) {
			throw new ModelException("Error parsing object fields", e);
		}
	}
	
	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O,?,V>, V> void parseObjectField(ObjectModel<O, M, F, ?, V> model, String field, BiConsumer<F, V> consumer) {
	//public <O, M extends Maker<O>> void parseObjectField(ObjectModel<O, M, ?, ?> definition, String name, BiConsumer<Field<O,?,?>, Object> consumer) {
		try {
			final var buf = this.input;
			while (true) {
				switch (buf.read()) {
					case -1:
						throw new ModelException("Model incomplete at "+buf.describe());
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
						break;
					case ':':
						F mfield = model.getField(field);
						if(mfield!=null) {
							consumer.accept(mfield, parseFieldValue(mfield));
						}
						else {
							parser.parse(models.anyModel);
						}
						return;
					default:
						throw new ModelException("Unexpected character at "+buf.describe());
				}
			}
		}
		catch(IOException e) {
			throw new ModelException("Error parsing object fields", e);
		}
	}

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> definition) {
		M maker = maker(definition);
		Model<I> valdef = definition.itemModel();
		Consumer<I> it = maker::item;
		final var buf = this.input;
		final var t = this.parser;
		try {
			LOOP:
			while (true) {
				switch (buf.read()) {
					case -1:
						throw new ModelException("Model incomplete at "+buf.describe());
					case ']':
						break LOOP;
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
					case ',':
						break;
					default:
						buf.unread();
						t.parseListItem(valdef, it);
				}
			}
		}
		catch(IOException e) {
			throw new ModelException("Error parsing list items", e);
		}
		return maker;
	}

	@Override
	public <T> T parseSimple(SimpleModel<T> definition) {
		try {
			return definition.parse(this.input, this.overflow);
		}
		catch(IOException e) {
			throw new ModelException("Error parsing value", e);
		}
	}

	@Override
	public Object parseNull() {
		var b = this.input;
		int c;
		try {
			if((c= b.read()) != 'n' || (c = b.read()) != 'u' || (c = b.read()) != 'l' || (c = b.read())!='l') {
				throw new UnexpectedPrimitiveException("Unexepected character in null "+(char)c);
			}
		}
		catch(IOException e) {
			throw new ModelException("Error parsing null", e);
		}
		return null;
	}

}