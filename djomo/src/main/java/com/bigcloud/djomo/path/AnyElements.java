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


public class AnyElements implements PathElement {
	final PathElement parent;
	
	public AnyElements(PathElement parent) {
		this.parent = parent;
	}

	@Override
	public PathElement getParent() {
		return parent;
	}

	@Override
	public boolean matches(PathElement path) {
		final PathElement p = parent;
		if(p instanceof RootElement) {
			// special case; any element under root element always matches, we don't need to recurse
			return true;
		}
		PathElement pathParent;
		while(true) {
			if(path==null) {
				return p.getParent() == null;
			}
			if(p.matches(path)) {
				return true;
			}
			pathParent = path.getParent();
			if(p.matches(pathParent)) {
				return true;
			}
			path = pathParent;
		}
	}

	@Override
	public String toString() {
		return "AnyElements [parent=" + parent + "]";
	}
}
