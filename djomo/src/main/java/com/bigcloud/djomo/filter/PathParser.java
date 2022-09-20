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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ObjectMaker;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.path.FieldElement;
import com.bigcloud.djomo.path.ListElement;
import com.bigcloud.djomo.path.PathElement;
import com.bigcloud.djomo.path.RootElement;
/**
 * Use PathParser.builder() to construct a PathParser with any number of other FilterParsers attached to path patterns.
 * 
 * @author Alex Vigdor
 *
 */
public class PathParser extends FilterParser {
	protected final PathElement[] patterns;
	protected FilterParser[] filters;
	protected Parser target;
	protected PathElement path = new RootElement();

	protected PathParser(PathFilter... pathFilters) {
		super();
		PathElement[] ps = new PathElement[pathFilters.length];
		FilterParser[] fs = new FilterParser[pathFilters.length];
		for (int i = 0; i < pathFilters.length; i++) {
			PathFilter pf = pathFilters[pathFilters.length - i - 1];
			ps[i] = pf.pattern;
			fs[i] = pf.filter;
		}
		patterns = ps;
		filters = fs;
	}

	private Parser filterStack() {
		Parser v = target;
		var p = path;
		var ps = patterns;
		var fs = filters;
		var psl = ps.length;
		for (int i = 0; i < psl; i++) {
			if (ps[i].matches(p)) {
				v = fs[i].parser(v);
			}
		}
		this.parser = v;
		return v;
	}

	@Override
	public PathParser parser(Parser parser) {
		this.target = parser;
		filterStack();
		return this;
	}

	@Override
	public <O, M extends ObjectMaker<O, F, V>, F extends Field<O, ?, V>, V> void parseObjectField(
			ObjectModel<O, M, F, ?, V> model, CharSequence field, BiConsumer<F, V> consumer) {
		final var parent = path;
		path = new FieldElement(parent, field.toString());
		filterStack().parseObjectField(model, field, consumer);
		path = parent;
	}

	@Override
	public <L, M extends ListMaker<L, I>, I> M parseList(ListModel<L, M, I> model) {
		final var parent = path;
		path = new ListElement(parent);
		try {
			return filterStack().parseList(model);
		} finally {
			path = parent;
		}
	}

	@Override
	public <T> void parseListItem(Model<T> model, Consumer<T> consumer) {
		filterStack().parseListItem(model, consumer);
		((ListElement) path).increment();
	}

	@Override
	public PathParser clone() {
		PathParser clone = (PathParser) super.clone();
		clone.target = null;
		clone.path = new RootElement();
		var fs = clone.filters.clone();
		clone.filters = fs;
		for (int i = 0; i < fs.length; i++) {
			fs[i] = fs[i].clone();
		}
		return clone;
	}

	private static class PathFilter {
		final PathElement pattern;
		final FilterParser filter;

		public PathFilter(String path, FilterParser filter) {
			this.pattern = PathElement.parse(path);
			this.filter = filter;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		List<PathFilter> pathFilters = new ArrayList<>();

		public Builder filter(String path, FilterParser filter) {
			pathFilters.add(new PathFilter(path, filter));
			return this;
		}

		public PathParser build() {
			return new PathParser(pathFilters.toArray(new PathFilter[pathFilters.size()]));
		}

		public boolean isEmpty() {
			return pathFilters.size() == 0;
		}
	}
}
