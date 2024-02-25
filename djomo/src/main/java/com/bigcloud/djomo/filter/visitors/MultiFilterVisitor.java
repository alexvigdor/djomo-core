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

import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.api.VisitorFilter;
import com.bigcloud.djomo.base.BaseVisitorFilter;
/**
 * Combine multiple FilterVisitors in order
 * 
 * @author Alex Vigdor
 *
 */
public class MultiFilterVisitor extends BaseVisitorFilter {
	BaseVisitorFilter[] filters;

	public MultiFilterVisitor(BaseVisitorFilter first, BaseVisitorFilter... filters) {
		this.filters = new BaseVisitorFilter[filters.length+1];
		this.filters[0]=first;
		System.arraycopy(filters, 0, this.filters, 1, filters.length);
	}

	@Override
	public void filter(Visitor visitor) {
		var f = filters;
		for (int i = f.length - 1; i >= 0; i--) {
			VisitorFilter vf = f[i];
			vf.filter(visitor);
			visitor = vf;
		}
		this.visitor = visitor;
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