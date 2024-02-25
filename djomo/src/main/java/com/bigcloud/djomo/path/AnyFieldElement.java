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

public class AnyFieldElement implements PathElement {
	final PathElement parent;
	
	public AnyFieldElement(PathElement parent) {
		this.parent = parent;
	}

	@Override
	public PathElement getParent() {
		return parent;
	}
	
	@Override
	public boolean matches(PathElement path) {
		if(path instanceof FieldElement fe) {
			return fe.name != null && parent.matches(path.getParent());
		}
		return false;
	}

	@Override
	public String toString() {
		return "AnyFieldElement [parent=" + parent + "]";
	}
}
