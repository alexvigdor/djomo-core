/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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
package com.bigcloud.djomo.object;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.bigcloud.djomo.annotation.Embed;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.base.BaseModel;
import com.bigcloud.djomo.filter.FilterField;
import com.bigcloud.djomo.internal.CharSequenceLookup;

public class EmbeddedObjectModel<T> extends BaseModel<T> implements ObjectModel<T> {
	final Field[] fields;
	final ObjectModel<?>[] embedModels;
	final Field[] embedFields;
	final CharSequenceLookup<Field> fieldLookup;
	final ObjectModel<T> baseModel;
	protected final FixedFieldVisitor fieldVisitor;

	public EmbeddedObjectModel(ObjectModel<T> baseModel, Type type, ModelContext context, Field[] fields,
			Field[] embedFields) {
		super(type, context);
		this.fields = fields;
		this.embedFields = embedFields;
		this.embedModels = Arrays.stream(embedFields).map(Field::model).toArray(ObjectModel[]::new);
		this.fieldVisitor = FixedFieldVisitor.visitorFor(fields);
		this.fieldLookup = new CharSequenceLookup<Field>(expandFieldMap(fields));
		this.baseModel = baseModel;
	}

	public static <T> ObjectModel<T> processEmbedModels(ObjectModel<T> model, Type type, ModelContext context) {
		var fields = model.fields();
		if (fields == null) {
			return model;
		}
		List<Field> fixedFields = new ArrayList<>();
		List<FieldMarker> dynamicEmbeds = new ArrayList<>();
		List<Field> embedFields = new ArrayList<>();
		for (var field : fields) {
			var embed = field.getAnnotation(Embed.class);
			if (embed != null) {
				embedFields.add(field);
				var fm = field.model();
				if (fm instanceof ObjectModel<?> ofm) {
					var nestedFields = ofm.fields();
					if (nestedFields == null) {
						dynamicEmbeds.add(new FieldMarker(field, embedFields.size()));
					} else {
						for (var nestedField : nestedFields) {
							fixedFields.add(new ChildField(field, nestedField, embedFields.size()));
						}
					}
				} else {
					throw new IllegalArgumentException(
							"Embedding objects requires the embedded model is an ObjectModel, but found non-ObjectModel "
									+ fm + " in field " + field.key());
				}
			} else {
				fixedFields.add(new ParentField(field));
			}
		}
		if (embedFields.isEmpty()) {
			return model;
		}
		var fixedAr = fixedFields.toArray(Field[]::new);
		var embedAr = embedFields.toArray(Field[]::new);
		if (dynamicEmbeds.isEmpty()) {
			return new EmbeddedObjectModel<T>(model, type, context, fixedAr, embedAr);
		}
		return new DynamicEmbeddedObjectModel<T>(model, type, context, fixedAr, embedAr,
				dynamicEmbeds.toArray(FieldMarker[]::new));
	}

	@Override
	public Object maker(T obj) {
		var m = maker();
		for (Field f : fields) {
			f.set(m, f.get(obj));
		}
		return m;
	}

	@Override
	public Object maker() {
		final var em = embedModels;
		final var len = em.length + 1;
		final var makers = new Object[len];
		makers[0] = baseModel.maker();
		for (int i = 1; i < len; i++) {
			makers[i] = em[i - 1].maker();
		}
		return makers;
	}

	@Override
	public T make(Object maker) {
		if (maker instanceof Object[] makers) {
			var parentMaker = makers[0];
			var efs = embedFields;
			var ems = embedModels;
			int expectedLen = efs.length + 1;
			if (makers.length == expectedLen) {
				for (int i = 1; i < expectedLen; i++) {
					efs[i - 1].set(parentMaker, ems[i - 1].make(makers[i]));
				}
			}
			return baseModel.make(parentMaker);
		}
		throw new IllegalArgumentException();
	}

	@Override
	public T parse(Parser parser) {
		return (T) parser.parseObject(this);
	}

	@Override
	public void visit(T obj, Visitor visitor) {
		visitor.visitObject(obj, this);
	}

	@Override
	public void forEachField(T t, BiConsumer consumer) {
		for (Field f : fields) {
			consumer.accept(f.key(), f.get(t));
		}
	}

	@Override
	public void visitFields(T t, Visitor visitor) {
		fieldVisitor.visitFields(t, visitor);
	}

	@Override
	public Field getField(CharSequence name) {
		return fieldLookup.get(name);
	}

	@Override
	public List<Field> fields() {
		return List.of(fields);
	}

	@Override
	public Stream<Field> fields(T instance) {
		return Stream.of(fields);
	}

	public static class ChildField extends FilterField {
		final private Field parentField;
		final private int makerIndex;

		public ChildField(Field parentField, Field childField, int makerIndex) {
			super(childField);
			this.parentField = parentField;
			this.makerIndex = makerIndex;
		}

		@Override
		public Object get(Object source) {
			return field.get(parentField.get(source));
		}

		@Override
		public void set(Object destination, Object value) {
			field.set(((Object[]) destination)[makerIndex], value);
		}

		@Override
		public void visit(Object source, Visitor visitor) {
			var child = parentField.get(source);
			if (child != null) {
				field.visit(child, visitor);
			}
		}

		@Override
		public void parse(Object destination, Parser parser) {
			field.parse(((Object[]) destination)[makerIndex], parser);
		}

		@Override
		public Field rekey(Object newKey) {
			return new ChildField(parentField, field.rekey(newKey), makerIndex);
		}

	}

	public static class ParentField extends FilterField {
		public ParentField(Field parentField) {
			super(parentField);
		}

		@Override
		public void set(Object destination, Object value) {
			field.set(((Object[]) destination)[0], value);
		}

		@Override
		public void parse(Object destination, Parser parser) {
			field.parse(((Object[]) destination)[0], parser);
		}

		@Override
		public Field rekey(Object newKey) {
			return new ParentField(field.rekey(newKey));
		}

	}

	public static class DynamicField extends FilterField {
		final private int makerIndex;

		public DynamicField(Field parentField, int makerIndex) {
			super(parentField);
			this.makerIndex = makerIndex;
		}

		@Override
		public void set(Object destination, Object value) {
			field.set(((Object[]) destination)[makerIndex], value);
		}

		@Override
		public void parse(Object destination, Parser parser) {
			field.parse(((Object[]) destination)[makerIndex], parser);
		}

		@Override
		public Field rekey(Object newKey) {
			return new DynamicField(field.rekey(newKey), makerIndex);
		}

	}

	private static class FieldMarker {
		private final Field field;
		private final int makerPos;

		private FieldMarker(Field field, int makerPos) {
			this.field = field;
			this.makerPos = makerPos;
		}
	}

	public static class DynamicEmbeddedObjectModel<T> extends EmbeddedObjectModel<T> {
		final FieldMarker[] dynamicEmbeds;

		public DynamicEmbeddedObjectModel(ObjectModel<T> baseModel, Type type, ModelContext context, Field[] fields,
				Field[] embedFields, FieldMarker[] dynamicEmbeds) {
			super(baseModel, type, context, fields, embedFields);
			this.dynamicEmbeds = dynamicEmbeds;
		}

		@Override
		public void forEachField(T t, BiConsumer consumer) {
			super.forEachField(t, consumer);
			for (FieldMarker fm : dynamicEmbeds) {
				var f = fm.field;
				var v = f.get(t);
				if (v != null) {
					((ObjectModel) f.model()).forEachField(v, consumer);
				}
			}
		}

		@Override
		public void visitFields(T t, Visitor visitor) {
			super.visitFields(t, visitor);
			for (FieldMarker fm : dynamicEmbeds) {
				var f = fm.field;
				var v = f.get(t);
				if (v != null) {
					((ObjectModel) f.model()).visitFields(v, visitor);
				}
			}
		}

		@Override
		public Field getField(CharSequence name) {
			Field f = super.getField(name);
			if (f == null) {
				for (FieldMarker d : dynamicEmbeds) {

					f = ((ObjectModel) d.field.model()).getField(name);
					if (f != null) {
						f = new DynamicField(f, d.makerPos);
						break;
					}
				}
			}
			return f;
		}

		@Override
		public Object maker(T obj) {
			Object[] m = (Object[]) super.maker(obj);
			for (FieldMarker d : dynamicEmbeds) {
				var field = d.field;
				var v = field.get(obj);
				var om = (ObjectModel) field.model();
				Object maker;
				if (v == null) {
					maker = om.maker();
				} else {
					maker = om.maker(v);
				}
				m[d.makerPos] = maker;
			}
			return m;
		}

		@Override
		public List<Field> fields() {
			return null;
		}

		@Override
		public Stream<Field> fields(T instance) {
			return baseModel.fields(instance);
		}
	}

}
