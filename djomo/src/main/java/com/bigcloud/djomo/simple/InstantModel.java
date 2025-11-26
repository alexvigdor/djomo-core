/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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
package com.bigcloud.djomo.simple;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.api.Visitor;

public class InstantModel extends DateTimeFormatterModel<Instant> {

	public InstantModel(ModelContext context) {
		super(Instant.class, context, DateTimeFormatter.ISO_INSTANT);
	}

	@Override
	public Instant parse(Parser parser) {
		var seq = parser.parseString();
		return DateTimeParser.parser(seq).getInstant();
	}
	
	@Override
	public void visit(Instant obj, Visitor visitor) {
		visitor.visitString(new DateTimePrinter.InstantDateTimePrinter(obj));
	}

}
