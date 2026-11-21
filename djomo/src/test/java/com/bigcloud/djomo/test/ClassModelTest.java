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
package com.bigcloud.djomo.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.StaticType;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.simple.ClassModel;

import lombok.Builder;
import lombok.Value;

/**
 * Tests for {@link ClassModel}, the allow-list-based codec for {@code Class<?>} fields.
 * <p>
 * The allow-list is enforced at parse time: only class names explicitly registered via
 * {@link ClassModel#allowing} are accepted. Bounds filtering silently discards entries
 * that do not conform to the field's generic bounds.
 */
public class ClassModelTest {

	private Json jsonWith(Class<?>... allowed) {
		return new Json(Models.builder()
				.factory(Class.class, ClassModel.allowing(allowed))
				.build());
	}

	// ── Fixtures ─────────────────────────────────────────────────────────────

	@Value
	@Builder
	public static class AnyClassBean {
		Class<?> type;
	}

	@Value
	@Builder
	public static class SubNumberBean {
		Class<? extends Number> type;
	}

	@Value
	@Builder
	public static class SuperNumberBean {
		Class<? super Number> type;
	}

	@Value
	@Builder
	public static class InvariantNumberBean {
		Class<Number> type;
	}

	@SuppressWarnings("rawtypes")
	@Value
	@Builder
	public static class RawClassBean {
		Class type;
	}

	@Value
	@Builder
	public static class TwoClassFieldsBean {
		Class<?> any;
		Class<? extends Number> sub;
	}

	@Value
	@Builder
	public static class GenericClassBean<T extends Number> {
		Class<T> type;
	}

	@Value
	@Builder
	public static class ParamBoundBean<T extends List<String>> {
		Class<T> type;
	}

	@Value
	@Builder
	public static class ArrayClassBean<T> {
		Class<T[]> type;
	}

	// legal java classes with (and without) a non-ascii name; Cafeé's name is
	// exactly Cafe's name plus a trailing é
	public static class Cafe {}

	public static class Cafeé {}

	// ── Positive ─────────────────────────────────────────────────────────────

	@Test
	public void testAnyClassRoundTrip() throws IOException {
		Json json = jsonWith(String.class);
		AnyClassBean bean = AnyClassBean.builder().type(String.class).build();
		String s = json.toString(bean);
		AnyClassBean rt = json.fromString(s, AnyClassBean.class);
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testMultipleAllowListEntries() throws IOException {
		Json json = jsonWith(String.class, Integer.class, UUID.class);
		for (Class<?> c : new Class<?>[] { String.class, Integer.class, UUID.class }) {
			AnyClassBean bean = AnyClassBean.builder().type(c).build();
			String s = json.toString(bean);
			AnyClassBean rt = json.fromString(s, AnyClassBean.class);
			Assert.assertEquals(rt, bean, "Round-trip failed for " + c.getName());
		}
	}

	@Test
	public void testSubNumberRoundTrip() throws IOException {
		Json json = jsonWith(Integer.class, Double.class);
		for (Class<? extends Number> c : List.of(Integer.class, Double.class )) {
			SubNumberBean bean = SubNumberBean.builder().type(c).build();
			String s = json.toString(bean);
			SubNumberBean rt = json.fromString(s, SubNumberBean.class);
			Assert.assertEquals(rt, bean, "Round-trip failed for " + c.getName());
		}
	}

	@Test
	public void testSuperNumberRoundTrip() throws IOException {
		Json json = jsonWith(Number.class, Object.class);
		for (Class<? super Number> c : List.<Class<? super Number>>of(Number.class, Object.class )) {
			SuperNumberBean bean = SuperNumberBean.builder().type(c).build();
			String s = json.toString(bean);
			SuperNumberBean rt = json.fromString(s, SuperNumberBean.class);
			Assert.assertEquals(rt, bean, "Round-trip failed for " + c.getName());
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testRawClassRoundTrip() throws IOException {
		Json json = jsonWith(String.class, Integer.class);
		for (Class<?> c : new Class<?>[] { String.class, Integer.class }) {
			RawClassBean bean = RawClassBean.builder().type(c).build();
			String s = json.toString(bean);
			RawClassBean rt = json.fromString(s, RawClassBean.class);
			Assert.assertEquals(rt, bean, "Round-trip failed for " + c.getName());
		}
	}

	@Test
	public void testInvariantNumberRoundTrip() throws IOException {
		Json json = jsonWith(Number.class);
		InvariantNumberBean bean = InvariantNumberBean.builder().type(Number.class).build();
		String s = json.toString(bean);
		InvariantNumberBean rt = json.fromString(s, InvariantNumberBean.class);
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testTwoClassFieldsIndependent() throws IOException {
		Json json = jsonWith(Integer.class, String.class);
		TwoClassFieldsBean bean = TwoClassFieldsBean.builder()
				.any(String.class)
				.sub(Integer.class)
				.build();
		String s = json.toString(bean);
		TwoClassFieldsBean rt = json.fromString(s, TwoClassFieldsBean.class);
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testConvert() {
		Models models = Models.builder()
				.factory(Class.class, ClassModel.allowing(String.class))
				.build();
		@SuppressWarnings("unchecked")
		Model<Class<?>> cm = (Model<Class<?>>) (Model<?>) models.get(Class.class);

		// Parse path: string in allow-list → Class
		Assert.assertEquals(cm.convert("java.lang.String"), String.class);

		// Parse path: string NOT in allow-list → IAE
		Assert.assertThrows(IllegalArgumentException.class,
				() -> cm.convert("java.util.UUID"));

		// Short-circuit: Class instance bypasses allow-list (in-memory, no class loading)
		Assert.assertEquals(cm.convert(UUID.class), UUID.class);
	}

	@Test
	public void testGenericClassRoundTrip() throws IOException {
		Json json = jsonWith(Number.class, Integer.class);
		GenericClassBean<Number> bean = GenericClassBean.<Number>builder().type(Number.class).build();
		String s = json.toString(bean);
		GenericClassBean<Number> rt = json.fromString(s, new StaticType<GenericClassBean<Number>>() {});
		Assert.assertEquals(rt, bean);
	}

	@Test
	public void testArrayTypeEntry() throws IOException {
		Json json = jsonWith(String[].class);
		AnyClassBean bean = AnyClassBean.builder().type(String[].class).build();
		String s = json.toString(bean);
		AnyClassBean rt = json.fromString(s, AnyClassBean.class);
		Assert.assertEquals(rt, bean);
	}

	// ── Negative ─────────────────────────────────────────────────────────────

	@Test
	public void testNotInAllowList() {
		Json json = jsonWith(String.class);
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.util.UUID\"}", AnyClassBean.class));
	}

	@Test
	public void testSubNumberDiscardsNonConforming() throws IOException {
		// String.class is not a Number subclass → silently discarded from allow-list
		Json json = jsonWith(String.class, Integer.class);

		// Integer conforms to Class<? extends Number> → round-trips
		SubNumberBean bean = SubNumberBean.builder().type(Integer.class).build();
		String s = json.toString(bean);
		SubNumberBean rt = json.fromString(s, SubNumberBean.class);
		Assert.assertEquals(rt, bean);

		// String was discarded → IAE
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.String\"}", SubNumberBean.class));
	}

	@Test
	public void testSubclassNotImplicitlyAllowed() {
		// Number.class is allow-listed; Integer is a subclass but NOT in the allow-list
		Json json = jsonWith(Number.class);
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.Integer\"}", SubNumberBean.class));
	}

	@Test
	public void testSuperclassNotImplicitlyAllowed() {
		// Integer.class is allow-listed; Number is a superclass but NOT in the allow-list
		Json json = jsonWith(Integer.class);
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.Number\"}", SubNumberBean.class));
	}

	@Test
	public void testInvariantMismatch() {
		// Class<Number> + allowing(Integer.class): Integer != Number → discarded → empty allow-list
		Json json = jsonWith(Integer.class);
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.Number\"}", InvariantNumberBean.class));
	}

	/**
	 * Default Models registers a {@link ClassModel} with an empty allow-list: class names
	 * are no longer loaded via forName, so an unregistered class fails closed with an IAE
	 * until {@link ClassModel#allowing} is used.
	 */
	@Test
	public void testDefaultModelsFailClosed() {
		Json json = new Json();
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.String\"}", AnyClassBean.class));
	}

	@Test
	public void testLowerBoundFilter() {
		// Class<? super Number> + allowing(String.class): String is not a super of Number → discarded
		Json json = jsonWith(String.class);
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.String\"}", SuperNumberBean.class));
	}

	/**
	 * A JSON {@code null} for a {@code Class<?>} field yields a {@code null} field
	 * (guarded at the top of {@link ClassModel#parse}).
	 */
	@Test
	public void testNullClassField() throws IOException {
		Json json = jsonWith(String.class);
		AnyClassBean rt = json.fromString("{\"type\":null}", AnyClassBean.class);
		Assert.assertNull(rt.getType());
	}

	// ── eraseToClass branch coverage ─────────────────────────────────────────
	//
	// The constructor's bounds/type-arg filtering reduces every type it sees to a Class
	// via the private eraseToClass. Using raw (unparameterized) bean references where
	// possible, each test below targets one remaining branch:
	//
	//   Class             – covered by the round-trips above
	//   ParameterizedType – <T extends List<String>> used raw (parameterized bound)
	//   GenericArrayType  – Class<T[]> field (generic array type arg)
	//   TypeVariable      – generic beans used raw (type variable type arg / bound)
	//   WildcardType      – no branch: satisfiesTypeArg unwraps wildcards to their
	//                       bounds before erasing, so eraseToClass never receives one
	//   Object.class      – defensive fallback for non-javac Type implementations;
	//                       fails closed (candidate mismatches, entry discarded)

	/**
	 * eraseToClass TypeVariable branch: {@code GenericClassBean<T extends Number>} is used
	 * raw, so the field's type argument is the type variable {@code T} itself; the bound
	 * ({@code Number}) is what erases and filters the allow-list.
	 */
	@Test
	public void testTypeVariableBoundErase() throws IOException {
		Json json = jsonWith(Number.class, String.class); // String discarded: not a Number

		GenericClassBean bean = GenericClassBean.<Number>builder().type(Number.class).build();
		String s = json.toString(bean);
		GenericClassBean rt = json.fromString(s, GenericClassBean.class); // raw: T remains a TypeVariable
		Assert.assertEquals(rt, bean);

		// String was discarded by the erased bound → IAE
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.String\"}", GenericClassBean.class));
	}

	/**
	 * eraseToClass ParameterizedType branch: {@code <T extends List<String>>} is used raw,
	 * so the type variable's bound is a parameterized type; it erases to {@code List.class}.
	 */
	@Test
	public void testParameterizedBoundErase() throws IOException {
		Json json = jsonWith(ArrayList.class, HashMap.class); // HashMap discarded: not a List

		ParamBoundBean bean = ParamBoundBean.<ArrayList>builder().type(ArrayList.class).build();
		String s = json.toString(bean);
		ParamBoundBean rt = json.fromString(s, ParamBoundBean.class); // raw: bound remains T
		Assert.assertEquals(rt, bean);

		// HashMap fails the erased bound → discarded → IAE
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.util.HashMap\"}", ParamBoundBean.class));
	}

	/**
	 * eraseToClass GenericArrayType branch: a field of {@code Class<T[]>} carries a generic
	 * array as its type argument; with {@code T = Object} it erases to {@code Object[].class}.
	 */
	@Test
	public void testGenericArrayClassField() throws IOException {
		Json json = jsonWith(Object[].class, String[].class); // String[] discarded: != Object[]

		ArrayClassBean bean = ArrayClassBean.<Object>builder().type(Object[].class).build();
		String s = json.toString(bean);
		ArrayClassBean rt = json.fromString(s, ArrayClassBean.class); // raw: component remains T
		Assert.assertEquals(rt, bean);

		// String[] was discarded → IAE
		Assert.assertThrows(IllegalArgumentException.class,
				() -> json.fromString("{\"type\":\"java.lang.String[]\"}", ArrayClassBean.class));
	}

	// ── Unicode class names ──────────────────────────────────────────────────
	//
	// Java identifiers may contain unicode letters, so class names like
	// ...$Cafeé are legal and allow-listable: the ClassModel's name lookup
	// matches non-ascii characters against their escape expansion, and a
	// non-allow-listed name is rejected with the standard allow-list error.

	/**
	 * A legal class with a non-ascii name must be allow-listable and
	 * round-trippable through the parser.
	 */
	@Test
	public void testUnicodeClassNameRoundTrip() throws IOException {
		Json json = jsonWith(Cafe.class, Cafeé.class);
		AnyClassBean bean = AnyClassBean.builder().type(Cafeé.class).build();
		String s = json.toString(bean);
		AnyClassBean rt = json.fromString(s, AnyClassBean.class);
		Assert.assertEquals(rt, bean);
	}

	/**
	 * A non-ascii class name that is not allow-listed must be rejected with
	 * the standard allow-list error. The query name extends the allow-listed
	 * {@code Cafe} name by a trailing é, so the lookup must miss (not fail)
	 * and the allow-list check must produce the rejection.
	 */
	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = ".*not in the parser allowlist.*")
	public void testUnicodeNameNotInAllowList() throws IOException {
		Json json = jsonWith(Cafe.class);
		json.fromString("{\"type\":\"" + Cafeé.class.getName() + "\"}", AnyClassBean.class);
	}
}
