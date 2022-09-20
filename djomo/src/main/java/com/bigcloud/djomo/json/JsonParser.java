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
import com.bigcloud.djomo.internal.CharSequenceParser;
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
			final var input = this.input;
			final var buf = input.buffer;
			final var t = this.parser;
			int rp = input.readPosition;
			int wp = input.writePosition;
			while (true) {
				if (rp == wp) {
					if (!input.refill()) {
						throw new ModelException("Model incomplete at " + input.describe());
					}
					rp = 0;
					wp = input.writePosition;
				}
				switch (buf[rp]) {
					case '{':
						input.readPosition = rp + 1;
						return parseObjectModel(definition);
					case '[':
						input.readPosition = rp + 1;
						return parseListModel(definition);
					case '"':
						input.readPosition = rp + 1;
						T o = (T) parseStringModel(definition);
						if (!(definition instanceof StringBasedModel) && !(definition instanceof StringModel)) {
							rp = input.readPosition;
							wp = input.writePosition;
							if (rp == wp && input.refill()) {
								rp = 0;
								wp = input.writePosition;
							}
							if (rp < wp && buf[rp] == '"') {
								input.readPosition = rp + 1;
							}
						}
						return o;
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
						rp++;
						break;
					case 't':
					case 'f':
						input.readPosition = rp;
						return (T) t.parseSimple(models.booleanModel);
					case 'n':
						input.readPosition = rp;
						return (T) t.parseNull();
					default:
						input.readPosition = rp;
						return parseNumberModel(definition);
				}
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing JSON", e);
		}
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> M parseObject(
			ObjectModel<O, M, F, ?, V> model) {
		final M maker = maker(model);
		final BiConsumer<F, V> consumer = maker::field;
		final var input = this.input;
		final var buf = input.buffer;
		final var t = this.parser;
		final var o = this.overflow;
		try {
			int rp = input.readPosition;
			int wp = input.writePosition;
			while (true) {
				if (rp == wp) {
					if (!input.refill()) {
						throw new ModelException("Model incomplete at " + input.describe());
					}
					rp = 0;
					wp = input.writePosition;
				}
				switch (buf[rp]) {
					case '"':
						input.readPosition = rp + 1;
						t.parseObjectField(model, CharSequenceParser.parse(input, o), consumer);
						rp = input.readPosition;
						wp = input.writePosition;
						break;
					case '}':
						input.readPosition = rp + 1;
						return maker;
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
					case ',':
						rp++;
						break;
					default:
						throw new ModelException("Unexpected character at " + input.describe());
				}
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing object fields", e);
		}
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F, ?, V> model, CharSequence field, BiConsumer<F, V> consumer) {
		try {
			F mfield = model.getField(field);
			final var buf = this.input;
			while (true) {
				switch (buf.read()) {
					case -1:
						throw new ModelException("Model incomplete at " + buf.describe());
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
						break;
					case ':':
						if (mfield != null) {
							consumer.accept(mfield, parseFieldValue(mfield));
						} else {
							parser.parse(models.anyModel);
						}
						return;
					default:
						throw new ModelException("Unexpected character at " + buf.describe());
				}
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing object fields", e);
		}
	}

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> definition) {
		M maker = maker(definition);
		Model<I> valdef = definition.itemModel();
		Consumer<I> it = maker::item;
		final var input = this.input;
		final var buf = input.buffer;
		final var t = this.parser;
		try {
			int rp = input.readPosition;
			int wp = input.writePosition;
			LOOP: while (true) {
				if (rp == wp) {
					if (!input.refill()) {
						throw new ModelException("Model incomplete at " + input.describe());
					}
					rp = 0;
					wp = input.writePosition;
				}
				switch (buf[rp]) {
					case ']':
						input.readPosition = rp + 1;
						break LOOP;
					case ' ':
					case '\t':
					case '\n':
					case '\r':
					case '\f':
					case ',':
						++rp;
						break;
					default:
						input.readPosition = rp;
						t.parseListItem(valdef, it);
						rp = input.readPosition;
						wp = input.writePosition;
				}
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing list items", e);
		}
		return maker;
	}

	@Override
	public <T> T parseSimple(SimpleModel<T> definition) {
		try {
			return definition.parse(this.input, this.overflow);
		} catch (IOException e) {
			throw new ModelException("Error parsing value", e);
		}
	}

	@Override
	public Object parseNull() {
		var b = this.input;
		int c;
		try {
			if ((c = b.read()) != 'n' || (c = b.read()) != 'u' || (c = b.read()) != 'l' || (c = b.read()) != 'l') {
				throw new UnexpectedPrimitiveException("Unexepected character in null " + (char) c);
			}
		} catch (IOException e) {
			throw new ModelException("Error parsing null", e);
		}
		return null;
	}

}