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
package com.bigcloud.djomo.filter.parsers;

import java.util.ArrayList;
import java.util.List;

import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.ListModel;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.ParserFilter;
import com.bigcloud.djomo.api.ParserFilterFactory;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.path.FieldElement;
import com.bigcloud.djomo.path.ListElement;
import com.bigcloud.djomo.path.PathElement;
import com.bigcloud.djomo.path.RootElement;

/**
 * Use PathParser.builder() to construct a PathParser with any number of other
 * FilterParsers attached to path patterns.
 * 
 * @author Alex Vigdor
 *
 */
public class PathParser extends BaseParserFilter {
	protected final PathElement[] patterns;
	protected ParserFilter[] filters;
	protected Parser target;
	protected PathElement path = new RootElement();

	protected PathParser(PathFilter... pathFilters) {
		super();
		PathElement[] ps = new PathElement[pathFilters.length];
		ParserFilter[] fs = new ParserFilter[pathFilters.length];
		for (int i = 0; i < pathFilters.length; i++) {
			PathFilter pf = pathFilters[pathFilters.length - i - 1];
			ps[i] = pf.pattern;
			fs[i] = pf.filter.newParserFilter();
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
				ParserFilter pf = fs[i];
				pf.filter(v);
				v = pf;
			}
		}
		this.parser = v;
		return v;
	}

	@Override
	public void filter(Parser parser) {
		this.target = parser;
		filterStack();
	}

	@Override
	public Object parseObject(ObjectModel model) {
		final var parent = path;
		path = new FieldElement(parent);
		var value = parser.parseObject(model);
		path = parent;
		return value;
	}

	@Override
	public Field parseObjectField(ObjectModel model, CharSequence field) {
		((FieldElement) path).setName(field.toString());
		return filterStack().parseObjectField(model, field);
	}

	@Override
	public Object parseList(ListModel model) {
		final var parent = path;
		path = new ListElement(parent);
		try {
			return parser.parseList(model);
		} finally {
			path = parent;
		}
	}

	@Override
	public void parseListItem() {
		filterStack().parseListItem();
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
			fs[i] = fs[i].newParserFilter();
		}
		return clone;
	}

	private static class PathFilter {
		final PathElement pattern;
		final ParserFilterFactory filter;

		public PathFilter(String path, ParserFilterFactory filter) {
			this.pattern = PathElement.parse(path);
			this.filter = filter;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		List<PathFilter> pathFilters = new ArrayList<>();

		public Builder filter(String path, ParserFilterFactory filter) {
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
