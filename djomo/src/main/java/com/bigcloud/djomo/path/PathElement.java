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
package com.bigcloud.djomo.path;

public interface PathElement {
	PathElement getParent();
	boolean matches(PathElement path);
	
	static PathElement parse(String path) {
		String[] parts = path.split("\\.|\\[");
		PathElement current = new RootElement();
		for(String part: parts) {
			if(part.endsWith("]")) {
				String index = part.substring(0, part.length() - 1);
				if ("*".equals(index)) {
					current = new AnyListElement(current);
				}
				else {
					ListElement el = new ListElement(current);
					el.setIndex(Integer.parseInt(index));
					current = el;
				}
			}
			else {
				if("*".equals(part)) {
					current = new AnyFieldElement(current);
				}
				else if("**".equals(part)) {
					current = new AnyElements(current);
				}
				else if(!part.isEmpty()){
					current = new FieldElement(current, part);
				}
			}
		}
		return current;
	}

}
