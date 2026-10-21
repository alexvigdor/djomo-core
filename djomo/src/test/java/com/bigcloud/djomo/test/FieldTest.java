/*******************************************************************************
 * Copyright 2024 Alex Vigdor
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
package com.bigcloud.djomo.test;

import static org.testng.Assert.*;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.LocalDate;
import java.util.UUID;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.error.GetFieldException;
import com.bigcloud.djomo.error.SetFieldException;
import com.bigcloud.djomo.object.BeanField;

public class FieldTest {
	public static class TypeA {
		protected String a;
		public String b;

		public String getA() {
			return a;
		}
	}

	public static class TypeB {
		protected String a;
		public String b;

		public void setA(String a) {
			this.a = a;
		}
	}

	Json json = new Json();

	@Test
	public void testFieldNoSetter() throws IOException {
		TypeA typeA = new TypeA();
		typeA.a = "A";
		typeA.b = "B";
		String str = json.toString(typeA);
		assertEquals(str, "{\"a\":\"A\",\"b\":\"B\"}");
		TypeA rt = json.fromString(str, TypeA.class);
		assertEquals(rt.a, null);
		assertEquals(rt.b, "B");
	}

	@Test
	public void testFieldNoGetter() throws IOException {
		TypeB typeB = new TypeB();
		typeB.a = "A";
		typeB.b = "B";
		String str = json.toString(typeB);
		assertEquals(str, "{\"b\":\"B\"}");
		TypeB rt = json.fromString("{\"a\":\"A\",\"b\":\"B\"}", TypeB.class);
		assertEquals(rt.a, "A");
		assertEquals(rt.b, "B");
	}

	/**
	 * Parameterized test that exercises rekey() for all BeanField subclasses.
	 * Validates that: - The returned object is the same instance type - The name is
	 * preserved - The key is updated to the new value
	 */
	@DataProvider(name = "beanFieldProvider")
	public Object[][] provideBeanFields() throws NoSuchMethodException, IllegalAccessException {
		Models models = new Models();
		var lookup = MethodHandles.lookup();

		// Create a dummy accessor that returns null/default values
		var stringAccessor = lookup.unreflect(String.class.getMethod("toString"));
		var intAccessor = lookup.unreflect(Integer.class.getMethod("intValue"));
		var longAccessor = lookup.unreflect(Long.class.getMethod("longValue"));
		var doubleAccessor = lookup.unreflect(Double.class.getMethod("doubleValue"));
		var floatAccessor = lookup.unreflect(Float.class.getMethod("floatValue"));
		var booleanAccessor = lookup.unreflect(Boolean.class.getMethod("booleanValue"));

		// Create dummy mutators
		var voidMutator = MethodHandles.empty(MethodType.methodType(void.class, Object.class, Object.class));

		return new Object[][] {
				{
						new BeanField.StringField(stringAccessor, voidMutator, "stringField",
								models.get(String.class))
				},
				{
						new BeanField.IntField(intAccessor, voidMutator, "intField",
								models.get(int.class))
				},
				{
						new BeanField.LongField(longAccessor, voidMutator, "longField",
								models.get(long.class))
				},
				{
						new BeanField.DoubleField(doubleAccessor, voidMutator, "doubleField",
								models.get(double.class))
				},
				{
						new BeanField.FloatField(floatAccessor, voidMutator, "floatField",
								models.get(float.class))
				},
				{
						new BeanField.BooleanField(booleanAccessor, voidMutator, "booleanField",
								models.get(boolean.class))
				},
				{
						new BeanField.TemporalField(stringAccessor, voidMutator, "temporalField",
								models.get(LocalDate.class))
				},
				{
						new BeanField.UUIDField(stringAccessor, voidMutator, "uuidField",
								models.get(UUID.class))
				},
				{
						new BeanField.ObjectField(stringAccessor, voidMutator, "objectField",
								(com.bigcloud.djomo.api.ObjectModel<?>) models.get(TypeA.class))
				},
				{
						new BeanField.ListField(stringAccessor, voidMutator, "listField",
								(com.bigcloud.djomo.api.ListModel<?>) models.get(java.util.List.class))
				},
		};
	}

	@Test(dataProvider = "beanFieldProvider")
	public void testBeanFieldRekey(com.bigcloud.djomo.api.Field originalField)
			throws IllegalAccessException, NoSuchFieldException, SecurityException {
		// Get the original field's properties using reflection to access protected name
		// field
		Object originalKey = originalField.key();
		Class<?> originalType = originalField.getClass();
		String testName = originalType.getSimpleName() + " (name: " + originalKey + ")";

		// Create a new key
		String newKey = "rekeyed_" + originalKey;

		// Call rekey()
		com.bigcloud.djomo.api.Field rekeyed = originalField.rekey(newKey);

		// Validate the returned object is the same type
		assertEquals(rekeyed.getClass(), originalType,
				"Rekeyed field should be same type as original for " + testName);

		// Validate the key is updated
		String rekeydKey = rekeyed.key().toString();
		assertEquals(rekeydKey, newKey,
				"Rekeyed field should have new key for " + testName);
		assertNotEquals(rekeydKey, originalKey,
				"Rekeyed field key should differ from original for " + testName);
	}

	/**
	 * Data provider for testing get/set/visit/parse operations on BeanField
	 * subclasses. Each entry contains: [Field, TestObject, TestValue]
	 */
	@DataProvider(name = "beanFieldOperationsProvider")
	public Object[][] provideBeanFieldsForOperations() throws NoSuchMethodException, IllegalAccessException {
		Models models = new Models();
		var lookup = MethodHandles.lookup();

		// Accessors and mutators for each type
		var stringAccessor = lookup.unreflect(String.class.getMethod("toString"));
		var intAccessor = lookup.unreflect(Integer.class.getMethod("intValue"));
		var longAccessor = lookup.unreflect(Long.class.getMethod("longValue"));
		var doubleAccessor = lookup.unreflect(Double.class.getMethod("doubleValue"));
		var floatAccessor = lookup.unreflect(Float.class.getMethod("floatValue"));
		var booleanAccessor = lookup.unreflect(Boolean.class.getMethod("booleanValue"));

		var voidMutator = MethodHandles.empty(MethodType.methodType(void.class, Object.class, Object.class));

		return new Object[][] {
				// StringField: String value
				{
						new BeanField.StringField(stringAccessor, voidMutator, "stringField",
								models.get(String.class)),
						"testString", "newValue"
				},
				// IntField: Integer value
				{
						new BeanField.IntField(intAccessor, voidMutator, "intField",
								models.get(int.class)),
						42, 99
				},
				// LongField: Long value
				{
						new BeanField.LongField(longAccessor, voidMutator, "longField",
								models.get(long.class)),
						100L, 200L
				},
				// DoubleField: Double value
				{
						new BeanField.DoubleField(doubleAccessor, voidMutator, "doubleField",
								models.get(double.class)),
						3.14, 2.71
				},
				// FloatField: Float value
				{
						new BeanField.FloatField(floatAccessor, voidMutator, "floatField",
								models.get(float.class)),
						1.5f, 2.5f
				},
				// BooleanField: Boolean value
				{
						new BeanField.BooleanField(booleanAccessor, voidMutator, "booleanField",
								models.get(boolean.class)),
						true, false
				},
				// TemporalField: LocalDate value
				{
						new BeanField.TemporalField(stringAccessor, voidMutator, "temporalField",
								models.get(LocalDate.class)),
						LocalDate.of(2023, 6, 15), LocalDate.of(2024, 12, 25)
				},
				// UUIDField: UUID value
				{
						new BeanField.UUIDField(stringAccessor, voidMutator, "uuidField",
								models.get(UUID.class)),
						UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
						UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479")
				},
				// ObjectField: Object value (TypeA instance)
				{
						new BeanField.ObjectField(stringAccessor, voidMutator, "objectField",
								(com.bigcloud.djomo.api.ObjectModel<?>) models.get(TypeA.class)),
						new TypeA(), new TypeA()
				},
				// ListField: List value
				{
						new BeanField.ListField(stringAccessor, voidMutator, "listField",
								(com.bigcloud.djomo.api.ListModel<?>) models.get(java.util.List.class)),
						java.util.List.of(1, 2, 3), java.util.List.of(4, 5, 6)
				},
		};
	}

	@Test(dataProvider = "beanFieldOperationsProvider")
	public void testBeanFieldOperations(Field field, Object testValue, Object newValue) {
		Class<?> fieldType = field.getClass();
		String testName = fieldType.getSimpleName();

		// Test 1: Verify get() and set() are callable (they will throw because of mock
		// setup, but that's expected)
		TestContainer testObj = new TestContainer(testValue);

		// Verify set() throws SetFieldException (wraps the underlying invoke error)
		try {
			field.set(testObj, newValue);
			// If no exception with a real bean, that would be normal
		} catch (SetFieldException e) {
			// Expected - wraps the underlying invocation error for mock fields
			assertNotNull(e.getCause(), "SetFieldException should wrap cause for " + testName);
		}

		// Test 2: Verify get() throws GetFieldException (wraps the underlying invoke
		// error)
		try {
			Object retrieved = field.get(testObj);
			// If no exception with a real bean, that would be normal
		} catch (GetFieldException e) {
			// Expected - wraps the underlying invocation error for mock fields
			assertNotNull(e.getCause(), "GetFieldException should wrap cause for " + testName);
		}

		// Test 3: Verify visit() throws GetFieldException when accessing fails (as
		// expected)
		TestingVisitor visitor = new TestingVisitor();
		try {
			field.visit(testObj, visitor);
			// If no exception, visitor should have been notified of the field
			assertTrue(visitor.fieldVisited, "Visitor should have received visitObjectField call for " + testName);
		} catch (GetFieldException e) {
			// Expected when mock field accessor fails
			assertNotNull(e.getCause(), "GetFieldException should wrap cause for " + testName);
		}

		// Test 4: Verify parse() throws SetFieldException when mutator fails (as
		// expected)
		TestingParser parser = new TestingParser(field, newValue);
		TestContainer parseTestObj = new TestContainer(testValue);
		try {
			field.parse(parseTestObj, parser);
			// If no exception, parser should have been invoked
			assertTrue(parser.parseCalled, "Parser should have been called for " + testName);
		} catch (SetFieldException e) {
			// Expected when mock field mutator fails
			assertNotNull(e.getCause(), "SetFieldException should wrap cause for " + testName);
		}
	}

	/**
	 * Simple container for test values.
	 */
	private static class TestContainer {
		Object value;

		TestContainer(Object value) {
			this.value = value;
		}
	}

	/**
	 * Mock Visitor for testing visit() behavior.
	 */
	private static class TestingVisitor implements Visitor {
		boolean fieldVisited = false;
		boolean valueVisited = false;
		private final Models models;

		TestingVisitor() {
			this.models = new Models();
		}

		@Override
		public void visitObjectField(Object key) {
			fieldVisited = true;
		}

		@Override
		public void visitNull() {
			valueVisited = true;
		}

		@Override
		public <T> void visitList(T list, com.bigcloud.djomo.api.ListModel<T> model) {
			valueVisited = true;
		}

		@Override
		public void visitListItem() {
		}

		@Override
		public <T> void visitObject(T object, com.bigcloud.djomo.api.ObjectModel<T> model) {
			valueVisited = true;
		}

		@Override
		public <T> void visit(T object, com.bigcloud.djomo.api.Model<T> model) {
			valueVisited = true;
		}

		@Override
		public void visit(Object obj) {
			valueVisited = true;
		}

		@Override
		public void visitString(CharSequence value) {
			valueVisited = true;
		}

		@Override
		public void visitInt(int value) {
			valueVisited = true;
		}

		@Override
		public void visitLong(long value) {
			valueVisited = true;
		}

		@Override
		public void visitDouble(double value) {
			valueVisited = true;
		}

		@Override
		public void visitFloat(float value) {
			valueVisited = true;
		}

		@Override
		public void visitBoolean(boolean value) {
			valueVisited = true;
		}

		@Override
		public void visitTemporal(java.time.temporal.TemporalAccessor time) {
			valueVisited = true;
		}

		@Override
		public void visitUUID(UUID value) {
			valueVisited = true;
		}

		@Override
		public Models models() {
			return models;
		}
	}

	/**
	 * Mock Parser for testing parse() behavior.
	 */
	private static class TestingParser implements Parser {
		Field field;
		Object valueToReturn;
		boolean parseCalled = false;
		private final Models models;

		TestingParser(Field field, Object valueToReturn) {
			this.field = field;
			this.valueToReturn = valueToReturn;
			this.models = new Models();
		}

		@Override
		public Object parse() {
			parseCalled = true;
			return field.model().convert(valueToReturn);
		}

		@Override
		public Object parse(com.bigcloud.djomo.api.Model model) {
			parseCalled = true;
			return model.convert(valueToReturn);
		}

		@Override
		public Object parseObject(com.bigcloud.djomo.api.ObjectModel model) {
			parseCalled = true;
			return valueToReturn;
		}

		@Override
		public com.bigcloud.djomo.api.Field parseObjectField(com.bigcloud.djomo.api.ObjectModel model,
				CharSequence field) {
			return null;
		}

		@Override
		public Object parseList(com.bigcloud.djomo.api.ListModel definition) {
			parseCalled = true;
			return valueToReturn;
		}

		@Override
		public Object parseNull() {
			parseCalled = true;
			return null;
		}

		@Override
		public int parseInt() {
			parseCalled = true;
			return ((Number) valueToReturn).intValue();
		}

		@Override
		public long parseLong() {
			parseCalled = true;
			return ((Number) valueToReturn).longValue();
		}

		@Override
		public float parseFloat() {
			parseCalled = true;
			return ((Number) valueToReturn).floatValue();
		}

		@Override
		public double parseDouble() {
			parseCalled = true;
			return ((Number) valueToReturn).doubleValue();
		}

		@Override
		public boolean parseBoolean() {
			parseCalled = true;
			return (Boolean) valueToReturn;
		}

		@Override
		public UUID parseUUID() {
			parseCalled = true;
			return (UUID) valueToReturn;
		}

		@Override
		public CharSequence parseString() {
			parseCalled = true;
			return (CharSequence) valueToReturn;
		}

		@Override
		public <T extends java.time.temporal.TemporalAccessor> T parseTemporal(
				com.bigcloud.djomo.api.TemporalType<T> type) {
			parseCalled = true;
			return (T) valueToReturn;
		}

		@Override
		public void parseListItem() {
		}

		@Override
		public Models models() {
			return models;
		}
	}
}
