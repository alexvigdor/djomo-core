package com.bigcloud.djomo.filter;

import java.util.List;
import java.util.function.BiConsumer;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Format;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;

public class FilterObjectModel<O> implements ObjectModel<O> {
	protected final ObjectModel<O> objectModel;
	public FilterObjectModel(ObjectModel<O> delegate) {
		this.objectModel = delegate;
	}

	@Override
	public Class<O> getType() {
		return objectModel.getType();
	}

	@Override
	public Format getFormat() {
		return objectModel.getFormat();
	}

	@Override
	public O convert(Object o) {
		return objectModel.convert(o);
	}

	@Override
	public O parse(Parser parser) {
		return objectModel.parse(parser);
	}

	@Override
	public void visit(O obj, Visitor visitor) {
		objectModel.visit(obj, visitor);
	}

	@Override
	public Models models() {
		return objectModel.models();
	}

	@Override
	public void forEachField(O t, BiConsumer consumer) {
		objectModel.forEachField(t, consumer);
	}

	@Override
	public void visitFields(O t, Visitor visitor) {
		objectModel.visitFields(t, visitor);
	}

	@Override
	public Field getField(CharSequence name) {
		return objectModel.getField(name);
	}

	@Override
	public List<Field> fields() {
		return objectModel.fields();
	}

	@Override
	public Object maker(O obj) {
		return objectModel.maker(obj);
	}

	@Override
	public Object maker() {
		return objectModel.maker();
	}

	@Override
	public O make(Object maker) {
		return objectModel.make(maker);
	}

}
