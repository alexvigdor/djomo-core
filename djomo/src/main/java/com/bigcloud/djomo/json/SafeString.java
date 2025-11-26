package com.bigcloud.djomo.json;


/**
 * Represents a json-safe char sequence that does not require escaping, for internal djomo use
 * 
 * @author Alex Vigdor
 *
 */
public class SafeString implements SafeCharSequence {
	final String str;
	final int len;
	
	public SafeString(String source) {
		str = source;
		len = source.length();
	}
	
	@Override
	public int length() {
		return len;
	}

	@Override
	public char charAt(int index) {
		return str.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new SafeString(str.substring(start, end));
	}
	
	@Override
	public String toString() {
		return str;
	}
	@Override
	public int hashCode() {
		return str.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof String s) {
			return str.equals(s);
		}
		if(o instanceof SafeString s) {
			return str.equals(s.str);
		}
		if(o instanceof CharSequence s) {
			if(len != s.length()){
				return false;
			}
			for(int i = 0; i< len; i++) {
				if(str.charAt(i) != s.charAt(i)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	@Override
	public int getChars(
			 char[] dst,
			 int dstBegin) {
		int l = len;
		str.getChars(0, l, dst, dstBegin);
		return dstBegin + l;
	}

}
