package com.bigcloud.djomo.json;

/**
 * For internal use only, allow marking the contents of a StringBuilder as json-safe characters that don't need escaping.
 * 
 * @author Alex Vigdor
 *
 */
public class SafeStringBuilder implements SafeCharSequence{
	final StringBuilder builder;
	final int len;
	
	public SafeStringBuilder(StringBuilder builder) {
		this.builder = builder;
		this.len = builder.length();
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public char charAt(int index) {
		return builder.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return builder.subSequence(start, end);
	}

	@Override
	public int getChars(char[] dst, int dstBegin) {
		var l = len;
		builder.getChars(0, l, dst, dstBegin);
		return dstBegin + l;
	}
	

	@Override
	public String toString() {
		return builder.toString();
	}
	@Override
	public int hashCode() {
		return builder.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof CharSequence s) {
			int len = builder.length();
			if(len != s.length()){
				return false;
			}
			for(int i = 0; i< len; i++) {
				if(builder.charAt(i) != s.charAt(i)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
