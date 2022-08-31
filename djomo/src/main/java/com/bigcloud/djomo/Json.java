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
package com.bigcloud.djomo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Arrays;

import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelFactory;
import com.bigcloud.djomo.base.AnnotationProcessor;
import com.bigcloud.djomo.filter.FilterParser;
import com.bigcloud.djomo.filter.FilterVisitor;
import com.bigcloud.djomo.filter.TypeParserFunction;
import com.bigcloud.djomo.filter.TypeVisitorFunction;
import com.bigcloud.djomo.io.Buffer;
import com.bigcloud.djomo.io.CharArraySink;
import com.bigcloud.djomo.io.Utf8StreamReader;
import com.bigcloud.djomo.io.Utf8StreamSink;
import com.bigcloud.djomo.io.WriterSink;
import com.bigcloud.djomo.json.IndentingJsonWriter;
import com.bigcloud.djomo.json.JsonParser;
import com.bigcloud.djomo.json.JsonWriter;
import com.bigcloud.djomo.json.MergeJsonParser;
/**
 * <p>
 * Primary high-level utility class for Djomo, used to read JSON data from byte arrays, input streams, readers or strings into java object models, 
 * and to write java object models out to JSON via binary streams, character streams or as strings.
 * </p><p>
 * The core Json implementation offers basic, efficient default handling for many data types. Read and write operations can be customized by applying
 * {@link FilterParser} and {@link FilterVisitor} instances that will intercept the call stack and can extend or modify the behavior of the parser or serializer.
 * </p><p>
 * Json supports two approaches to applying {@link FilterParser} and {@link FilterVisitor} to read and write operations; they can be applied ad-hoc to 
 * any read or write operation as varargs passed at the end of the method call.  They can also be wired into the Json object permanently
 * for full-time use by using a Json.JsonBuilder; it offers visit() and parse() methods for registering filters, and a scan() method for processing 
 * {@link com.bigcloud.djomo.annotation.Visit} and {@link com.bigcloud.djomo.annotation.Parse} annotations to construct filters.  The order of filters
 * matters; filters are invoked in the order they are added or declared.  Filters passed in as varargs to a read or write method are executed 
 * before filters that were attached to Json via a builder.
 * </p><p>
 * Json is a lightweight object that relies on an underlying {@link Models} object; you can share the same Models object across multiple Json
 * instances that have been configured with different filters. To add support for data types that don't work with Djomo out of the box,
 * you can add a custom {@link ModelFactory} to the {@link Models} passed into the Json, or you can use {@link TypeVisitorFunction} and {@link TypeParserFunction}
 * to apply custom type mapping functions to this Json.
 * </p>
 * 
 * @author Alex Vigdor
 *
 */
public class Json {
	private static final ThreadLocal<char[]> readBuffer = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[8192];
		}
	};
	private static final ThreadLocal<char[]> parseBuffer = new ThreadLocal<char[]>() {
		public char[] initialValue() {
			return new char[4096];
		}
	};

	private final Models models;
	private final FilterVisitor[] visitorFilters;
	private final FilterParser[] parserFilters;
	private final AnnotationProcessor annotationProcessor;

	public Json() {
		this(new Models());
	}

	public Json(Models models) {
		this(models, new AnnotationProcessor(models), new FilterVisitor[0], new FilterParser[0]);
	}

	public Json(Models models, AnnotationProcessor annotationProcessor, FilterVisitor[] visitorFilters, FilterParser[] parserFilters) {
		this.models = models;
		this.visitorFilters = visitorFilters.clone();
		this.parserFilters = parserFilters.clone();
		this.annotationProcessor = annotationProcessor;
	}

	public Models models() {
		return models;
	}

	public Object read(Reader reader, FilterParser... filters) throws IOException {
		return read(reader, (Model) null, filters);
	}

	public <T> T read(Reader reader, Class<T> type, FilterParser... filters) throws IOException {
		Model<T> model = models.get(type);
		return (T) read(reader, model, filters);
	}

	public <T> T read(Reader reader, StaticType<T> type, FilterParser... filters) throws IOException {
		Model<T> model = models.get(type);
		return type.getStaticType().cast(read(reader, model, filters));
	}

	public Object read(Reader reader, Type type, FilterParser... filters) throws IOException {
		Model<?> model = models.get(type);
		return read(reader, model, filters);
	}

	public <T> T read(Reader reader, T destination, FilterParser... filters) throws IOException {
		var rb = new Buffer(readBuffer.get(), reader);
		var pb = new Buffer(parseBuffer.get());
		if (destination == null) {
			return (T) new JsonParser(models, rb, pb, filters).filter(models.anyModel);
		}
		var def = models.get(destination.getClass());
		return (T) new MergeJsonParser(models, rb, pb, destination, filters(filters)).filter(def);
	}

	private <T> T read(Reader reader, Model<T> definition, FilterParser... filters) {
		var rb = new Buffer(readBuffer.get(), reader);
		var pb = new Buffer(parseBuffer.get());
		if (definition == null) {
			return (T) new JsonParser(models, rb, pb, filters).filter(models.anyModel);
		}
		return new JsonParser(models, rb, pb, filters(filters)).filter(definition);
	}

	public Object read(InputStream in, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), filters);
	}

	public <T> T read(InputStream in, Class<T> type, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), type, filters);
	}

	public <T> T read(InputStream in, StaticType<T> type, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), type, filters);
	}

	public Object read(InputStream in, Type type, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), type, filters);
	}

	public <T> T read(InputStream in, T destination, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), destination, filters);
	}

	public Object read(byte[] in, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), filters);
	}

	public <T> T read(byte[] in, Class<T> type, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), type, filters);
	}

	public <T> T read(byte[] in, StaticType<T> type, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), type, filters);
	}

	public Object read(byte[] in, Type type, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), type, filters);
	}

	public <T> T read(byte[] in, T destination, FilterParser... filters) throws IOException {
		return read(new Utf8StreamReader(in), destination, filters);
	}

	public void write(Object data, Writer writer, FilterVisitor... filters) {
		try (var jw = new JsonWriter(models, new WriterSink(writer), filters(filters))) {
			jw.filter(data);
		}
	}

	public void write(Object data, Writer writer, String indentChars, FilterVisitor... filters) {
		try (var jw = new IndentingJsonWriter(models, new WriterSink(writer), indentChars, filters(filters))) {
			jw.filter(data);
		}
	}

	public void write(Object data, OutputStream out, FilterVisitor... filters) {
		try (var jw = new JsonWriter(models, new Utf8StreamSink(out), filters(filters))) {
			jw.filter(data);
		}
	}

	public void write(Object data, OutputStream out, String indentChars, FilterVisitor... filters) {
		try (var jw = new IndentingJsonWriter(models, new Utf8StreamSink(out), indentChars, filters(filters))) {
			jw.filter(data);
		}
	}

	public Object fromString(String json, FilterParser... filters) throws IOException {
		return read(new StringReader(json), filters);
	}

	public <T> T fromString(String json, Class<T> type, FilterParser... filters) throws IOException {
		return read(new StringReader(json), type, filters);
	}

	public <T> T fromString(String json, StaticType<T> type, FilterParser... filters) throws IOException {
		return read(new StringReader(json), type, filters);
	}

	public Object fromString(String json, Type type, FilterParser... filters) throws IOException {
		return read(new StringReader(json), type, filters);
	}

	public <T> T fromString(String json, T destination, FilterParser... filters) throws IOException {
		return read(new StringReader(json), destination, filters);
	}

	public String toString(Object data, FilterVisitor... filters) {
		var sink = new CharArraySink();
		try (var jw = new JsonWriter(models, sink, filters(filters))) {
			jw.filter(data);
		}
		return sink.toString();
	}

	public String toString(Object data, String indent, FilterVisitor... filters) {
		var sink = new CharArraySink();
		try (var jw = new IndentingJsonWriter(models, sink, indent, filters(filters))) {
			jw.filter(data);
		}
		return sink.toString();
	}

	private FilterVisitor[] filters(FilterVisitor... filters) {
		int numFilters = 0;
		if (filters != null) {
			numFilters = filters.length;
		}
		var vf = visitorFilters;
		var vfl = vf.length;
		var nl = numFilters + vfl;
		if (nl == 0) {
			// empty array
			return vf;
		}
		if (vfl == 0) {
			return filters;
		}
		if (filters != null) {
			filters = Arrays.copyOf(filters, nl);
		} else {
			filters = new FilterVisitor[nl];
		}
		for (int i = numFilters; i < nl; i++) {
			filters[i] = vf[i - numFilters].clone();
		}
		return filters;
	}

	private FilterParser[] filters(FilterParser... filters) {
		int numFilters = 0;
		if (filters != null) {
			numFilters = filters.length;
		}
		var pf = parserFilters;
		var pfl = pf.length;
		var nl = numFilters + pfl;
		if (nl == 0) {
			// empty array
			return pf;
		}
		if (pfl == 0) {
			return filters;
		}
		if (filters != null) {
			filters = Arrays.copyOf(filters, nl);
		} else {
			filters = new FilterParser[nl];
		}
		for (int i = numFilters; i < nl; i++) {
			filters[i] = pf[i - numFilters].clone();
		}
		return filters;
	}

	public static JsonBuilder builder() {
		return new JsonBuilder();
	}

	public AnnotationProcessor getAnnotationProcessor() {
		return annotationProcessor;
	}

	/**
	 * Builder to customize a Json with a {@link Models}, and any number of {@link FilterVisitor} and {@link FilterParser}.
	 * Order of filters matters; filters will be invoked in the order added or declared in a scanned class.
	 */
	public static class JsonBuilder {
		Models models;
		AnnotationProcessor processor;
		ArrayDeque<FilterVisitor> visitorFilters = new ArrayDeque<>();
		ArrayDeque<FilterParser> parserFilters = new ArrayDeque<>();

		public JsonBuilder models(Models models) {
			this.models = models;
			this.processor = new AnnotationProcessor(models);
			return this;
		}

		public JsonBuilder visit(FilterVisitor... filters) {
			for (FilterVisitor v : filters) {
				visitorFilters.add(v);
			}
			return this;
		}

		public JsonBuilder parse(FilterParser... filters) {
			for (FilterParser p : filters) {
				parserFilters.add(p);
			}
			return this;
		}
		/**
		 * Scan java classes for {@link com.bigcloud.djomo.annotation.Visit} and {@link com.bigcloud.djomo.annotation.Parse} annotations, load and attach declared filters to the built Json instance
		 * 
		 * @param classes The java classes to scan for {@literal @Parse} and {@literal @Visit} annotations
		 * @return this builder
		 */
		public JsonBuilder scan(Class<?>... classes) {
			if(processor == null) {
				models(new Models());
			}
			for (Class c : classes) {
				for (FilterVisitor v : processor.visitorFilters(c)) {
					visitorFilters.add(v);
				}
				for (FilterParser f : processor.parserFilters(c)) {
					parserFilters.add(f);
				}
			}
			return this;
		}

		public Json build() {
			Models m = models == null ? new Models() : models;
			AnnotationProcessor p = processor == null ? new AnnotationProcessor(m) : processor;
			return new Json(m, p, visitorFilters.toArray(new FilterVisitor[0]),
					parserFilters.toArray(new FilterParser[0]));
		}
	}
}
