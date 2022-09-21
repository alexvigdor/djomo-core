package com.bigcloud.djomo.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A normal map lookup with a char sequence requires two traversals; one to
 * compute the hash code, and then a char-by-char confirmation during equals.
 * This lookup is intended for modest sets of reasonably different names as
 * found on typical objects; it uses binary search to find a match on the first
 * character, and then performs a single traversal of the char sequence combined
 * with binary search to locate the matching value if any.
 * 
 * Maybe we should try a collapsed Trie structure, where nodes are only broken
 * down as far as common suffix; so discrete names will be a flat list. We could
 * even consider using a bitmask to test whether a node contains a descendant of
 * the target length
 * 
 * @author Alex Vigdor
 *
 * @param <T>
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
		int pos = 0;
		//System.out.println("Lookup.get("+cs+", "+pos+") from "+this);
		int csl = cs.length();
		CharArrayLookup<T> node = this;
		while (node != null) {
			char[] np = node.prefix;
			if (np.length > 0) {
				int pm = 0;
				for (; pm < np.length && pos < csl; pm++) {
					if (np[pm] != cs.charAt(pos)) {
						return null;
					}
					pos++;
				}
				if (pm != np.length) {
					return null;
				} 
			}
			if (pos == csl) {
				return node.data;
			}
			char first = cs.charAt(pos++);
			if (first < 32 || first > 126) {
				throw new IllegalArgumentException("Character out of range for CharArrayLookup " + first);
			}
			node = node.children[first - ' '];
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
