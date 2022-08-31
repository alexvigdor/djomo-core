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
package com.bigcloud.djomo.filter;
/**
 * Prevent null field values or list items from being visited
 * 
 * @author Alex Vigdor
 *
 */
public class OmitNullVisitor extends FilterVisitor {
	@Override
	public void visitObjectField(Object name, Object value) {
		if (value != null) {
			visitor.visitObjectField(name, value);
		}
	}

	@Override
	public void visitListItem(Object o) {
		if(o != null) {
			visitor.visitListItem(o);
		}
	}
}
