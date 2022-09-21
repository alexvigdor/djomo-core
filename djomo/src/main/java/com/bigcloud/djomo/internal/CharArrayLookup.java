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
public class CharArrayLookup<T>  {
	LookupNode<T> lookup;

	public CharArrayLookup(Map<CharSequence, T> data) {
		lookup = new LookupNode();
		data.forEach((k, t) -> lookup.set(k, 0, t));
	}

	/**
	 * Algorithm: use binary search to find any key starting with the same
	 * character. Keep matching characters until one doesn't match, and use diff to
	 * decide to search up or down keys sequentially.
	 * 
	 * @param key
	 * @return
	 */
	public T get(CharSequence key) {
		return lookup.get(key,0);
	}

	public static class LookupNode<T> {
		char[] prefix = null;
		//todo would it be worth the add-time expense of copying array contents to make this field final?
		LookupNode<T>[] children = new LookupNode[95];
		T data;

		void set(CharSequence cs, int pos, T data) {
			//System.out.println("Lookup.set("+cs+", "+pos+") to "+this);
			int csl = cs.length();
			if(prefix == null) {
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
					LookupNode<T> split = new LookupNode();
					split.children = this.children;
					this.children =  new LookupNode[95];
					split.prefix = Arrays.copyOfRange(prefix, pm+1, prefix.length);
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
			LookupNode<T> dest = children[first - ' '];
			if(dest == null) {
				dest = new LookupNode<>();
				children[first-' ']=dest;
			}
			dest.set(cs, pos, data);
		}
		
		T get(CharSequence cs, int pos) {
			//System.out.println("Lookup.get("+cs+", "+pos+") from "+this);
			int csl = cs.length();
			if (prefix.length > 0) {
				int pm = 0;
				for (; pm < prefix.length && pos < csl; pm++) {
					if (prefix[pm] != cs.charAt(pos)) {
						return null;
					}
					pos++;
				}
				if (pm == prefix.length) {
					if (pos == csl) {
						return data;
					} 
				} else {
					return null;
				}
			}
			if(pos == csl) {
				return data;
			}
			char first = cs.charAt(pos++);
			if (first < 32 || first > 126) {
				throw new IllegalArgumentException("Character out of range for CharArrayLookup " + first);
			}
			LookupNode<T> dest = children[first - ' '];
			if(dest == null) {
				return null;
			}
			return dest.get(cs, pos);
		}

		public String toString() {
			Map<String, LookupNode<T>> lettermappings = new HashMap<>();
			for(int i=0; i< children.length;i++) {
				LookupNode<T> ln = children[i];
				if(ln != null) {
					lettermappings.put(String.valueOf((char) (i+' ')), ln);
				}
			}
			return "[ prefix: " + (prefix == null ? null : new String(prefix)) + ", data: " + data + ", children: "
					+ lettermappings + " ]";
		}
	}
	
	public String toString() {
		return lookup.toString();
	}
}
