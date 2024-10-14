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
package com.bigcloud.djomo.filter.visitors;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;

public class BlackHoleVisitor implements VisitorFilter{

	@Override
	public void visitNull() {
	}

	@Override
	public <T> void visitList(T list, ListModel<T> model) {
	}

	@Override
	public void visitListItem() {
	}

	@Override
	public <T> void visitObject(T object, ObjectModel<T> model) {
	}

	@Override
	public void visitObjectField(Object name) {
	}

	@Override
	public void visit(Object obj) {
	}
	
	@Override
	public <T> void visit(T obj, Model<T> model) {
	}

	@Override
	public void visitInt(int value) {
	}

	@Override
	public void visitLong(long value) {
	}

	@Override
	public void visitFloat(float value) {
	}

	@Override
	public void visitDouble(double value) {
	}

	@Override
	public void visitBoolean(boolean value) {
	}

	@Override
	public void visitString(CharSequence value) {
	}

	@Override
	public Models models() {
		return null;
	}

	@Override
	public VisitorFilter newVisitorFilter() {
		return this;
	}

	@Override
	public void filter(Visitor visitor) {
		
	}

}
