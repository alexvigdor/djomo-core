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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Rename fields from how the Model Field is named, to what is found in the Json output.
 * 
 * @author Alex Vigdor
 *
 */
public class RenameVisitor extends FilterVisitor {
	private final ConcurrentHashMap<String, String> nameMappings;

	public RenameVisitor(String... args) {
		this.nameMappings = new ConcurrentHashMap<>();
		for (int i = 0; i < (args.length - 1); i += 2) {
			nameMappings.put(args[i], args[i + 1]);
		}
	}

	public RenameVisitor(Map<String, String> nameMappings) {
		this.nameMappings = new ConcurrentHashMap<>(nameMappings);
	}

	@Override
	public void visitObjectField(Object name, Object value) {
		String n = name.toString();
		visitor.visitObjectField(nameMappings.getOrDefault(n, n), value);
	}

}
