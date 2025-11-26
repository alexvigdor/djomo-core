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
package com.bigcloud.djomo.object;

import java.util.Arrays;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Visitor;

public abstract class FixedFieldVisitor {

	public abstract void visitFields(Object obj, Visitor visitor);

	public static FixedFieldVisitor visitorFor(Field[] fields) {
		return switch (fields.length) {
			case 0 -> new F0();
			case 1 -> new F1(fields);
			case 2 -> new F2(fields);
			case 3 -> new F3(fields);
			case 4 -> new F4(fields);
			case 5 -> new F5(fields);
			case 6 -> new F6(fields);
			case 7 -> new F7(fields);
			case 8 -> new F8(fields);
			case 9 -> new F9(fields);
			case 10 -> new F10(fields);
			default -> new Fe(fields);
		};
	}

	static class Fe extends F10 {
		protected final FixedFieldVisitor overflow;

		private Fe(Field... fields) {
			super(fields);
			overflow = visitorFor(Arrays.copyOfRange(fields, 10, fields.length));
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			super.visitFields(obj, visitor);
			overflow.visitFields(obj, visitor);
		}

	}

	static class F0 extends FixedFieldVisitor {

		@Override
		public void visitFields(Object obj, Visitor visitor) {
		}

	}

	static class F1 extends FixedFieldVisitor {
		protected final Field f1;

		private F1(Field... fields) {
			this.f1 = fields[0];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
		}

	}

	static class F2 extends F1 {
		protected final Field f2;

		private F2(Field... fields) {
			super(fields);
			this.f2 = fields[1];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
		}

	}

	static class F3 extends F2 {
		protected final Field f3;

		private F3(Field... fields) {
			super(fields);
			this.f3 = fields[2];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
		}

	}

	static class F4 extends F3 {
		protected final Field f4;

		private F4(Field... fields) {
			super(fields);
			this.f4 = fields[3];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
		}

	}

	static class F5 extends F4 {
		protected final Field f5;

		private F5(Field... fields) {
			super(fields);
			this.f5 = fields[4];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
			f5.visit(obj, visitor);
		}

	}

	static class F6 extends F5 {
		protected final Field f6;

		private F6(Field... fields) {
			super(fields);
			this.f6 = fields[5];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
			f5.visit(obj, visitor);
			f6.visit(obj, visitor);
		}

	}

	static class F7 extends F6 {
		protected final Field f7;

		private F7(Field... fields) {
			super(fields);
			this.f7 = fields[6];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
			f5.visit(obj, visitor);
			f6.visit(obj, visitor);
			f7.visit(obj, visitor);
		}

	}

	static class F8 extends F7 {
		protected final Field f8;

		private F8(Field... fields) {
			super(fields);
			this.f8 = fields[7];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
			f5.visit(obj, visitor);
			f6.visit(obj, visitor);
			f7.visit(obj, visitor);
			f8.visit(obj, visitor);
		}

	}

	static class F9 extends F8 {
		protected final Field f9;

		private F9(Field... fields) {
			super(fields);
			this.f9 = fields[8];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
			f5.visit(obj, visitor);
			f6.visit(obj, visitor);
			f7.visit(obj, visitor);
			f8.visit(obj, visitor);
			f9.visit(obj, visitor);
		}

	}

	static class F10 extends F9 {
		protected final Field f10;

		private F10(Field... fields) {
			super(fields);
			this.f10 = fields[9];
		}

		@Override
		public void visitFields(Object obj, Visitor visitor) {
			f1.visit(obj, visitor);
			f2.visit(obj, visitor);
			f3.visit(obj, visitor);
			f4.visit(obj, visitor);
			f5.visit(obj, visitor);
			f6.visit(obj, visitor);
			f7.visit(obj, visitor);
			f8.visit(obj, visitor);
			f9.visit(obj, visitor);
			f10.visit(obj, visitor);
		}

	}
}
