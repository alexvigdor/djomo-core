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

import java.util.Set;
/**
 * Prevents fields from being visited. Can be wrapped in a {@link TypeVisitor} or {@link PathVisitor}.
 * 
 * @author Alex Vigdor
 *
 */
public class ExcludeVisitor extends FilterVisitor {
	Set<String> excludes;

	public ExcludeVisitor(String... fields) {
		excludes = Set.of(fields);
	}

	public boolean exclude(String fieldName) {
		if (excludes.isEmpty()) {
			return true;
		}
		return excludes.contains(fieldName);
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		if (!exclude(name.toString())) {
			visitor.visitObjectField(name, value);
		}
	}
}
