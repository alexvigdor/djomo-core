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

import com.bigcloud.djomo.api.Visitor;
/**
 * Combine multiple FilterVisitors in order
 * 
 * @author Alex Vigdor
 *
 */
public class MultiFilterVisitor extends FilterVisitor {
	FilterVisitor[] filters;

	public MultiFilterVisitor(FilterVisitor... filters) {
		this.filters = filters;
	}

	@Override
	public FilterVisitor visitor(Visitor visitor) {
		var f = filters;
		for (int i = f.length - 1; i >= 0; i--) {
			visitor = f[i].visitor(visitor);
		}
		this.visitor = visitor;
		return this;
	}

	public MultiFilterVisitor clone() {
		MultiFilterVisitor clone = (MultiFilterVisitor) super.clone();
		var fs = filters.clone();
		clone.filters = fs;
		for (int i = 0; i < fs.length; i++) {
			fs[i] = fs[i].clone();
		}
		return clone;
	}
}