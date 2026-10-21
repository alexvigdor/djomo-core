package com.bigcloud.djomo.test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.filter.FilterListModel;
import com.bigcloud.djomo.filter.FilterModel;
import com.bigcloud.djomo.filter.FilterObjectModel;

public class FilterModelTest {
	@Test
	public void testFilterModel() {
		boolean[] invoked = new boolean[5];
		Model<String> validator = new FilterModel<String>(null) {
			@Override
			public String convert(Object o) {
				invoked[0] = true;
				return "";
			}

			@Override
			public String parse(Parser parser) {
				invoked[1] = true;
				return "";
			}

			@Override
			public void visit(String obj, Visitor visitor) {
				invoked[2] = true;
			}

			@Override
			public Models models() {
				invoked[3] = true;
				return null;
			}

			@Override
			public void tryVisit(String obj, Visitor visitor) {
				invoked[4] = true;
			}
		};
		Model<String> testModel = new FilterModel<String>(validator) {
		};
		testModel.convert("test");
		Assert.assertTrue(invoked[0]);
		testModel.parse(null);
		Assert.assertTrue(invoked[1]);
		testModel.visit("test", null);
		Assert.assertTrue(invoked[2]);
		testModel.models();
		Assert.assertTrue(invoked[3]);
		testModel.tryVisit("test", null);
		Assert.assertTrue(invoked[4]);
	}

	@Test
	public void testFilterListModel() {
		boolean[] invoked = new boolean[9];
		ListModel<List<String>> validator = new FilterListModel<List<String>>(null) {
			@Override
			public Object maker(List<String> obj) {
				invoked[0] = true;
				return null;
			}

			@Override
			public Object maker() {
				invoked[1] = true;
				return null;
			}

			@Override
			public void forEachItem(List<String> t, Consumer consumer) {
				invoked[2] = true;
			}

			@Override
			public Stream stream(List<String> t) {
				invoked[3] = true;
				return null;
			}

			@Override
			public Model itemModel() {
				invoked[4] = true;
				return null;
			}

			@Override
			public void visitItems(List<String> t, Visitor visitor) {
				invoked[5] = true;
			}

			@Override
			public void parseItem(Object listMaker, Parser parser) {
				invoked[6] = true;
			}

			@Override
			public List<String> make(Object maker) {
				invoked[7] = true;
				return null;
			}

			@Override
			public List<String> parse(Parser parser) {
				invoked[8] = true;
				return null;
			}

		};
		ListModel<List<String>> testModel = new FilterListModel<List<String>>(validator) {
		};
		testModel.maker(null);
		Assert.assertTrue(invoked[0]);
		testModel.maker();
		Assert.assertTrue(invoked[1]);
		testModel.forEachItem(null, null);
		Assert.assertTrue(invoked[2]);
		testModel.stream(null);
		Assert.assertTrue(invoked[3]);
		testModel.itemModel();
		Assert.assertTrue(invoked[4]);
		testModel.visitItems(null, null);
		Assert.assertTrue(invoked[5]);
		testModel.parseItem(null, null);
		Assert.assertTrue(invoked[6]);
		testModel.make(null);
		Assert.assertTrue(invoked[7]);
		testModel.parse(null);
		Assert.assertTrue(invoked[8]);
	}

	@Test
	public void testFilterObjectModel() {
		boolean[] invoked = new boolean[8];
		ObjectModel<Object> validator = new FilterObjectModel<Object>(null) {

			@Override
			public void forEachField(Object t, BiConsumer consumer) {
				invoked[0] = true;
			}

			@Override
			public void visitFields(Object t, Visitor visitor) {
				invoked[1] = true;
			}

			@Override
			public Field getField(CharSequence name) {
				invoked[2] = true;
				return null;
			}

			@Override
			public List<Field> fields() {
				invoked[3] = true;
				return null;
			}

			@Override
			public Stream<Field> fields(Object obj) {
				invoked[4] = true;
				return null;
			}

			@Override
			public Object maker(Object obj) {
				invoked[5] = true;
				return null;
			}

			@Override
			public Object maker() {
				invoked[6] = true;
				return null;
			}

			@Override
			public Object make(Object maker) {
				invoked[7] = true;
				return null;
			}
		};
		ObjectModel<Object> testModel = new FilterObjectModel<Object>(validator) {
		};
		testModel.forEachField(null, null);
		Assert.assertTrue(invoked[0]);
		testModel.visitFields(null, null);
		Assert.assertTrue(invoked[1]);
		testModel.getField(null);
		Assert.assertTrue(invoked[2]);
		testModel.fields();
		Assert.assertTrue(invoked[3]);
		testModel.fields(null);
		Assert.assertTrue(invoked[4]);
		testModel.maker(null);
		Assert.assertTrue(invoked[5]);
		testModel.maker();
		Assert.assertTrue(invoked[6]);
		testModel.make(null);
		Assert.assertTrue(invoked[7]);
	}
}
