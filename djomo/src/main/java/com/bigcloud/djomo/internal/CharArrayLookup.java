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
package com.bigcloud.djomo.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A normal map lookup with a char sequence requires two traversals of the
 * characters; one to compute the hash code, and then a char-by-char
 * confirmation during equals. This lookup is intended for modest sets of
 * reasonably different names as found on typical objects; it uses a precomputed
 * trie data structure to lookup a value with only a single traversal of the
 * characters in the key. Only String keys with a subset of ASCII characters are
 * supported (decimal 32-126 inclusive).
 * 
 * @author Alex Vigdor
 *
 * @param <T> The type of value to be looked up with a simple ascii string
 */
public class CharArrayLookup<T> {

	private CharArrayLookup() {

	}

	public CharArrayLookup(Map<CharSequence, T> data) {
		data.forEach((k, t) -> set(k, 0, t));
	}

	private char[] prefix = null;
	// todo would it be worth the add-time expense of copying array contents to make
	// this field final?
	private CharArrayLookup<T>[] children = new CharArrayLookup[95];
	private T data;

	private void set(CharSequence cs, int pos, T data) {
		// System.out.println("Lookup.set("+cs+", "+pos+") to "+this);
		int csl = cs.length();
		if (prefix == null) {
			// this node is empty, fill the prefix and data
			prefix = new char[csl - pos];
			for (int i = pos; i < csl; i++) {
				char c = cs.charAt(i);
				if (c < 32 || c > 126) {
					throw new IllegalArgumentException("Character out of range for CharArrayLookup " + c);
				}
				prefix[i - pos] = c;
			}
			this.data = data;
			return;
		}
		if (prefix.length > 0) {
			int pm = 0;
			for (; pm < prefix.length && pos < csl; pm++) {
				if (prefix[pm] != cs.charAt(pos)) {
					break;
				}
				pos++;
			}
			if (pm == prefix.length) {
				if (pos == csl) {
					// this is the right destination for the data
					this.data = data;
					return;
				}
			} else {
				// we need to split this prefix apart
				CharArrayLookup<T> split = new CharArrayLookup<>();
				split.children = this.children;
				this.children = new CharArrayLookup[95];
				split.prefix = Arrays.copyOfRange(prefix, pm + 1, prefix.length);
				split.data = this.data;
				this.children[prefix[pm] - ' '] = split;
				prefix = Arrays.copyOfRange(prefix, 0, pm);
				if (pos == csl) {
					// we need to capture this value on the first half of the split
					this.data = data;
					return;
				}
				this.data = null;
			}
		}
		char first = cs.charAt(pos++);
		if (first < 32 || first > 126) {
			throw new IllegalArgumentException("Character out of range for CharArrayLookup " + first);
		}
		CharArrayLookup<T> dest = children[first - ' '];
		if (dest == null) {
			dest = new CharArrayLookup<>();
			children[first - ' '] = dest;
		}
		dest.set(cs, pos, data);
	}

	public T get(CharSequence cs) {
		int csl = cs.length();
		if (csl == 0) {
			if (prefix.length == 0) {
				return data;
			}
			return null;
		}
		char c = cs.charAt(0);
		int pos = 1;
		CharArrayLookup<T> node = this;
		while (node != null) {
			char[] np = node.prefix;
			if (np.length > 0) {
				for (int pm = 0; pm < np.length; pm++) {
					if (np[pm] != c) {
						return null;
					}
					if (pos == csl) {
						if (pm < np.length - 1) {
							return null;
						}
						return node.data;
					}
					c = cs.charAt(pos++);
				}
			}
			if (c < 32 || c > 126) {
				throw new IllegalArgumentException("Character out of range for CharArrayLookup " + c);
			}
			node = node.children[c - ' '];
			if (pos == csl) {
				return node == null ? null : node.data;
			}
			c = cs.charAt(pos++);
		}
		return null;
	}

	public String toString() {
		Map<String, CharArrayLookup<T>> lettermappings = new HashMap<>();
		for (int i = 0; i < children.length; i++) {
			CharArrayLookup<T> ln = children[i];
			if (ln != null) {
				lettermappings.put(String.valueOf((char) (i + ' ')), ln);
			}
		}
		return "[ prefix: " + (prefix == null ? null : new String(prefix)) + ", data: " + data + ", children: "
				+ lettermappings + " ]";
	}
}
