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
 * characters in the key.
 * <p>
 * Keys may contain any character above decimal 126, such as the unicode letters
 * allowed in java identifiers. Each such character is normalized to its
 * 6-character {@code &#92;uXXXX} escape sequence so the trie itself only stores
 * the 32-126 range; lookups apply the same expansion to non-ascii characters in
 * the query without allocating. Control characters (decimal 0-31) cannot appear
 * in valid java names and are not supported: a key containing one fails
 * construction with {@link IllegalArgumentException}, and a query containing one
 * is a miss, returning null rather than failing.
 * <p>
 * The lookup compares characters exactly, without unicode normalization: a
 * composed {@code é} does not match a decomposed {@code e} plus combining acute
 * accent. Note that a key containing the literal characters (backslash, u, and
 * four hex digits, the text of the escape above) normalize to the same form as
 * the key containing the actual é character, so the two alias each other; such
 * keys are outside the supported key space as backslash is not a valid java
 * identifier character.
 *
 * @author Alex Vigdor
 *
 * @param <T> The type of value to be looked up with a char sequence
 */
public class CharSequenceLookup<T> {

	/**
	 * hex digit lookup table, the same idiom used by {@code BaseJsonWriter} to emit
	 * string escapes one character at a time without a scratch buffer.
	 */
	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };

	private CharSequenceLookup() {

	}

	public CharSequenceLookup(Map<CharSequence, T> data) {
		data.forEach((k, t) -> set(normalize(k), 0, t));
		if (prefix == null) {
			prefix = new char[0];
		}
	}

	private char[] prefix = null;
	// todo would it be worth the add-time expense of copying array contents to make
	// this field final?
	private CharSequenceLookup<T>[] children = new CharSequenceLookup[95];
	private T data;

	/**
	 * Returns the 1-based {@code step}-th character of the 6-character
	 * {@code &#92;uXXXX} escape expansion of a character above 126, using the same
	 * hex digit math as {@code BaseJsonWriter} string escaping.
	 */
	private static char escapeChar(char c, int step) {
		final char[] hex = HEX;
		switch (step) {
		case 1:
			return '\\';
		case 2:
			return 'u';
		case 3:
			return hex[c / 4096];
		case 4:
			return hex[c / 256 % 16];
		case 5:
			return hex[c / 16 % 16];
		default:
			return hex[c % 16];
		}
	}

	/**
	 * Converts a key to the form the trie stores: each character in the 32-126
	 * range is copied through as-is and each character above 126 is expanded to its
	 * {@code &#92;uXXXX} escape. Two passes: one to size the result exactly, one to
	 * fill it. A character below 32 fails with {@link IllegalArgumentException}
	 * before any allocation.
	 */
	private static char[] normalize(CharSequence cs) {
		final int csl = cs.length();
		int size = 0;
		for (int i = 0; i < csl; i++) {
			char c = cs.charAt(i);
			if (c < 32) {
				throw new IllegalArgumentException("Character out of range for CharSequenceLookup " + c);
			}
			size += c > 126 ? 6 : 1;
		}
		char[] out = new char[size];
		int o = 0;
		final char[] hex = HEX;
		for (int i = 0; i < csl; i++) {
			char c = cs.charAt(i);
			if (c <= 126) {
				out[o++] = c;
			} else {
				out[o++] = '\\';
				out[o++] = 'u';
				out[o++] = hex[c / 4096];
				c %= 4096;
				out[o++] = hex[c / 256];
				c %= 256;
				out[o++] = hex[c / 16];
				c %= 16;
				out[o++] = hex[c];
			}
		}
		return out;
	}

	/**
	 * Inserts a value under a key that has already been through
	 * {@link #normalize(CharSequence)}, so every character is in the 32-126 range
	 * and valid for the {@link #children} table.
	 */
	private void set(char[] cs, int pos, T data) {
		// System.out.println("Lookup.set("+cs+", "+pos+") to "+this);
		final int csl = cs.length;
		if (prefix == null) {
			// this node is empty, fill the prefix and data
			prefix = Arrays.copyOfRange(cs, pos, csl);
			this.data = data;
			return;
		}
		if (prefix.length > 0) {
			int pm = 0;
			for (; pm < prefix.length && pos < csl; pm++) {
				if (prefix[pm] != cs[pos]) {
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
		char first = cs[pos++];
		CharSequenceLookup<T> dest = children[first - ' '];
		if (dest == null) {
			dest = new CharSequenceLookup<>();
			children[first - ' '] = dest;
		}
		dest.set(cs, pos, data);
	}

	/**
	 * Looks up a value by key. Characters above 126 in the key are matched against
	 * their {@code &#92;uXXXX} expansion without allocating.
	 *
	 * @return the value under the key, or {@code null} if there is no entry for
	 * the key, including when the key contains a control character (decimal
	 * 0-31): the lookup is tolerant of such queries, while a key containing one
	 * would have failed construction
	 */
	public T get(CharSequence cs) {
		final int csl = cs.length();
		if (csl == 0) {
			return prefix.length == 0 ? data : null;
		}
		if (cs instanceof CharArraySequence cas) {
			return getAscii(cas.buffer, null, cas.start, cas.start + csl);
		}
		return getAscii(null, cs, 0, csl);
	}

	/**
	 * The single-pass, allocation-free lookup walk. Exactly one of {@code buf}
	 * and {@code cs} is non-null; {@code pos} is the index of the next character
	 * to read and {@code end} the exclusive end of the window (absolute indices
	 * into {@code buf} or {@code charAt} offsets into {@code cs}). Each node
	 * compares the next character against its prefix, then selects a child; the
	 * walk stops when the stream or the trie does. A character above 126 diverts
	 * to {@link #getEscaped} to match its 6-character escape expansion, a stream
	 * shorter than a node prefix diverts to {@link #getAsciiShortOrEscaped}, and
	 * a control character returns null rather than failing: the unsigned
	 * comparison of the child index catches both cases in one test, because a
	 * negative index wraps above 94.
	 */
	private T getAscii(final char[] buf, final CharSequence cs, int pos, final int end) {
		CharSequenceLookup<T> node = this;

		while (node != null) {
			final char[] np = node.prefix;
			final int npl = np.length;

			if (npl > 0) {
				if (end - pos < npl) {
					return getAsciiShortOrEscaped(buf, cs, pos, end, node);
				}
				for (int pm = 0; pm < npl; pm++) {
					final char c = buf != null ? buf[pos++] : cs.charAt(pos++);
					if (np[pm] != c) {
						return c > 126 ? getEscaped(buf, cs, pos - 1, end, node, pm) : null;
					}
				}
			}

			if (pos == end) {
				return node.data;
			}

			final char c = buf != null ? buf[pos++] : cs.charAt(pos++);
			final int idx = c - ' ';
			if ((char) idx > 94) {
				if (c > 126) {
					return getEscaped(buf, cs, pos - 1, end, node, npl);
				}
				return null;
			}

			node = node.children[idx];
		}
		return null;
	}

	private T getEscaped(final char[] buf, final CharSequence cs, int pos, final int end,
			CharSequenceLookup<T> node, int pm) {
		// Continues a lookup walk after a character above 126: at entry
		// buf[pos] / cs.charAt(pos) is that character. The walk resumes by
		// emitting its 6-character escape expansion (backslash, u, four hex
		// digits) one character at a time; step is how many have been emitted
		// (0 when the next comparison needs a fresh character from the stream),
		// and raw is the character being expanded. pm resumes prefix matching at
		// the index of the prefix character the fallback character diverged
		// from; pm == prefix.length means the prefix was fully consumed, so the
		// first escape character goes straight to the child select.
		char raw = buf != null ? buf[pos++] : cs.charAt(pos++);
		int step = 1;
		char c = '\\';

		while (node != null) {
			final char[] np = node.prefix;
			final int npl = np.length;

			if (pm < npl) {
				for (; pm < npl; pm++) {
					if (np[pm] != c) {
						return null;
					}
					if (pos >= end && step == 0) {
						return pm < npl - 1 ? null : node.data;
					}
					if (step == 0) {
						c = buf != null ? buf[pos++] : cs.charAt(pos++);
						if (c > 126) {
							raw = c;
							step = 1;
							c = '\\';
						}
					} else {
						c = escapeChar(raw, step + 1);
						step = (step == 5) ? 0 : step + 1;
					}
				}
			}

			if (c < 32) {
				return null;
			}
			node = node.children[c - ' '];
			pm = 0;

			if (pos >= end && step == 0) {
				return node != null && node.prefix.length == 0 ? node.data : null;
			}
			if (step == 0) {
				c = buf != null ? buf[pos++] : cs.charAt(pos++);
				if (c > 126) {
					raw = c;
					step = 1;
					c = '\\';
				}
			} else {
				c = escapeChar(raw, step + 1);
				step = (step == 5) ? 0 : step + 1;
			}
		}
		return null;
	}

	/**
	 * Lookup continuation when the remaining stream is shorter than the current
	 * node's prefix, so the stored key extends past the query. A character above
	 * 126 still diverts to {@link #getEscaped} because its 6-character
	 * expansion can still match the prefix, but when the stream runs out the
	 * result is null: the query ends before the stored key does.
	 */
	private T getAsciiShortOrEscaped(final char[] buf, final CharSequence cs, int pos, final int end,
			CharSequenceLookup<T> node) {
		final char[] np = node.prefix;
		int pm = 0;
		while (pos < end) {
			final char c = buf != null ? buf[pos++] : cs.charAt(pos++);
			if (np[pm] != c) {
				return c > 126 ? getEscaped(buf, cs, pos - 1, end, node, pm) : null;
			}
			pm++;
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