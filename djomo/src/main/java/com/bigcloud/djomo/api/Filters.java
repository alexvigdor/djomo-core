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
package com.bigcloud.djomo.api;

import com.bigcloud.djomo.api.parsers.BooleanParser;
import com.bigcloud.djomo.api.parsers.DoubleParser;
import com.bigcloud.djomo.api.parsers.FloatParser;
import com.bigcloud.djomo.api.parsers.IntParser;
import com.bigcloud.djomo.api.parsers.ListParser;
import com.bigcloud.djomo.api.parsers.LongParser;
import com.bigcloud.djomo.api.parsers.ModelParser;
import com.bigcloud.djomo.api.parsers.ObjectParser;
import com.bigcloud.djomo.api.parsers.StringParser;
import com.bigcloud.djomo.api.visitors.BooleanVisitor;
import com.bigcloud.djomo.api.visitors.DoubleVisitor;
import com.bigcloud.djomo.api.visitors.FloatVisitor;
import com.bigcloud.djomo.api.visitors.IntVisitor;
import com.bigcloud.djomo.api.visitors.ListVisitor;
import com.bigcloud.djomo.api.visitors.LongVisitor;
import com.bigcloud.djomo.api.visitors.ModelVisitor;
import com.bigcloud.djomo.api.visitors.ObjectVisitor;
import com.bigcloud.djomo.api.visitors.StringVisitor;

/**
 * Convenience method for converting functional interface implementations into
 * visitor filters.
 * 
 * @author Alex Vigdor
 *
 */
public interface Filters {
	static VisitorFilter visitInt(IntVisitor intVisitor) {
		return intVisitor.newVisitorFilter();
	}

	static VisitorFilter visitBoolean(BooleanVisitor booleanVisitor) {
		return booleanVisitor.newVisitorFilter();
	}

	static VisitorFilter visitLong(LongVisitor longVisitor) {
		return longVisitor.newVisitorFilter();
	}

	static VisitorFilter visitFloat(FloatVisitor floatVisitor) {
		return floatVisitor.newVisitorFilter();
	}

	static VisitorFilter visitDouble(DoubleVisitor doubleVisitor) {
		return doubleVisitor.newVisitorFilter();
	}

	static VisitorFilter visitString(StringVisitor stringVisitor) {
		return stringVisitor.newVisitorFilter();
	}

	static VisitorFilter visitList(ListVisitor listVisitor) {
		return listVisitor.newVisitorFilter();
	}

	static VisitorFilter visitObject(ObjectVisitor<?> objectVisitor) {
		return objectVisitor.newVisitorFilter();
	}

	static <T> VisitorFilter visitObject(Class<T> type, ObjectVisitor<T> objectVisitor) {
		return objectVisitor.newVisitorFilter(type);
	}

	static VisitorFilter visitModel(ModelVisitor<?> modelVisitor) {
		return modelVisitor.newVisitorFilter();
	}

	static <T> VisitorFilter visitModel(Class<T> type, ModelVisitor<T> modelVisitor) {
		return modelVisitor.newVisitorFilter(type);
	}

	static ParserFilter parseInt(IntParser intParser) {
		return intParser.newParserFilter();
	}

	static ParserFilter parseBoolean(BooleanParser booleanParser) {
		return booleanParser.newParserFilter();
	}

	static ParserFilter parseLong(LongParser longParser) {
		return longParser.newParserFilter();
	}

	static ParserFilter parseFloat(FloatParser floatParser) {
		return floatParser.newParserFilter();
	}

	static ParserFilter parseDouble(DoubleParser doubleParser) {
		return doubleParser.newParserFilter();
	}

	static ParserFilter parseList(ListParser listParser) {
		return listParser.newParserFilter();
	}

	static ParserFilter parseString(StringParser stringParser) {
		return stringParser.newParserFilter();
	}

	static ParserFilter parseObject(ObjectParser<?> objectParser) {
		return objectParser.newParserFilter();
	}

	static <T> ParserFilter parseObject(Class<T> type, ObjectParser<T> objectParser) {
		return objectParser.newParserFilter(type);
	}

	static ParserFilter parseModel(ModelParser<?> objectParser) {
		return objectParser.newParserFilter();
	}

	static <T> ParserFilter parseModel(Class<T> type, ModelParser<T> objectParser) {
		return objectParser.newParserFilter(type);
	}
}
