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

import java.util.ArrayList;
import java.util.List;

import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Visitor;
import com.bigcloud.djomo.path.FieldElement;
import com.bigcloud.djomo.path.ListElement;
import com.bigcloud.djomo.path.PathElement;
import com.bigcloud.djomo.path.RootElement;
/**
 * Use PathVisitor.builder() to construct a PathVisitor with any number of other FilterVisitors attached to path patterns.
 * 
 * @author Alex Vigdor
 *
 */
public class PathVisitor extends FilterVisitor {
	protected final PathElement[] patterns;
	protected FilterVisitor[] filters;
	protected Visitor target;
	protected PathElement path = new RootElement();

	protected PathVisitor(PathFilter... pathFilters) {
		super();
		PathElement[] ps = new PathElement[pathFilters.length];
		FilterVisitor[] fs = new FilterVisitor[pathFilters.length];
		for (int i = 0; i < pathFilters.length; i++) {
			PathFilter pf = pathFilters[pathFilters.length - i - 1];
			ps[i] = pf.pattern;
			fs[i] = pf.filter;
		}
		patterns = ps;
		filters = fs;
	}

	private Visitor filterStack() {
		Visitor v = target;
		var p = path;
		var ps = patterns;
		var fs = filters;
		var psl = ps.length;
		for (int i = 0; i < psl; i++) {
			if (ps[i].matches(p)) {
				v = fs[i].visitor(v);
			}
		}
		this.visitor = v;
		return v;
	}

	@Override
	public PathVisitor visitor(Visitor visitor) {
		this.target = visitor;
		filterStack();
		return this;
	}

	@Override
	public <T> void visitList(T model, ListModel<T, ?, ?> definition) {
		final var parent = path;
		path = new ListElement(parent);
		filterStack().visitList(model, definition);
		path = parent;
	}

	@Override
	public void visitListItem(Object obj) {
		filterStack().visitListItem(obj);
		((ListElement) path).increment();
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		final var parent = path;
		path = new FieldElement(parent, name.toString());
		filterStack().visitObjectField(name, value);
		path = parent;
	}

	@Override
	public PathVisitor clone() {
		PathVisitor clone = (PathVisitor) super.clone();
		clone.target = null;
		clone.path = new RootElement();
		var fs = clone.filters.clone();
		clone.filters = fs;
		for (int i = 0; i < fs.length; i++) {
			fs[i] = fs[i].clone();
		}
		return clone;
	}

	protected static class PathFilter {
		final PathElement pattern;
		final FilterVisitor filter;

		protected PathFilter(String path, FilterVisitor filter) {
			this.pattern = PathElement.parse(path);
			this.filter = filter;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		List<PathFilter> pathFilters = new ArrayList<>();

		public Builder filter(String path, FilterVisitor filter) {
			pathFilters.add(new PathFilter(path, filter));
			return this;
		}

		public PathVisitor build() {
			return new PathVisitor(pathFilters.toArray(new PathFilter[pathFilters.size()]));
		}

		public boolean isEmpty() {
			return pathFilters.size() == 0;
		}
	}

}
