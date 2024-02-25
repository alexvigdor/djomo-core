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
public class CharSequenceLookup<T> {

	private CharSequenceLookup() {

	}

	public CharSequenceLookup(Map<CharSequence, T> data) {
		data.forEach((k, t) -> set(k, 0, t));
		if (prefix == null) {
			prefix = new char[0];
		}
	}

	private char[] prefix = null;
	// todo would it be worth the add-time expense of copying array contents to make
	// this field final?
	private CharSequenceLookup<T>[] children = new CharSequenceLookup[95];
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
					throw new IllegalArgumentException("Character out of range for CharSequenceLookup " + c);
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
				CharSequenceLookup<T> split = new CharSequenceLookup<>();
				split.children = this.children;
				this.children = new CharSequenceLookup[95];
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
			throw new IllegalArgumentException("Character out of range for CharSequenceLookup " + first);
		}
		CharSequenceLookup<T> dest = children[first - ' '];
		if (dest == null) {
			dest = new CharSequenceLookup<>();
			children[first - ' '] = dest;
		}
		dest.set(cs, pos, data);
	}

	public T get(CharSequence cs) {
		final int csl = cs.length();
		if (csl == 0) {
			if (prefix.length == 0) {
				return data;
			}
			return null;
		}
		if(cs instanceof CharArraySequence cas) {
			var buf = cas.buffer.buffer;
			var pos = cas.start;
			var end = pos + csl;
			char c = buf[pos++];
			CharSequenceLookup<T> node = this;
			while (node != null) {
				final char[] np = node.prefix;
				final int npl = np.length;
				if (npl > 0) {
					for (int pm = 0; pm < npl ; pm++) {
						if (np[pm] != c) {
							return null;
						}
						if (pos == end) {
							if (pm < npl - 1) {
								return null;
							}
							return node.data;
						}
						c = buf[pos++];
					}
				}
				if (c < 32 || c > 126) {
					throw new IllegalArgumentException("Character out of range for CharSequenceLookup " + c);
				}
				node = node.children[c - ' '];
				if (pos == end) {
					return node == null ? null : node.data;
				}
				c = buf[pos++];
			}
		}
		else {
			char c = cs.charAt(0);
			int pos = 1;
			CharSequenceLookup<T> node = this;
			while (node != null) {
				final char[] np = node.prefix;
				final int npl = np.length;
				if (npl > 0) {
					for (int pm = 0; pm < npl ; pm++) {
						if (np[pm] != c) {
							return null;
						}
						if (pos == csl) {
							if (pm < npl - 1) {
								return null;
							}
							return node.data;
						}
						c = cs.charAt(pos++);
					}
				}
				if (c < 32 || c > 126) {
					throw new IllegalArgumentException("Character out of range for CharSequenceLookup " + c);
				}
				node = node.children[c - ' '];
				if (pos == csl) {
					return node == null ? null : node.data;
				}
				c = cs.charAt(pos++);
			}
		}
		return null;
	}

	public String toString() {
		Map<String, CharSequenceLookup<T>> lettermappings = new HashMap<>();
		for (int i = 0; i < children.length; i++) {
			CharSequenceLookup<T> ln = children[i];
			if (ln != null) {
				lettermappings.put(String.valueOf((char) (i + ' ')), ln);
			}
		}
		return "[ prefix: " + (prefix == null ? null : new String(prefix)) + ", data: " + data + ", children: "
				+ lettermappings + " ]";
	}
}
