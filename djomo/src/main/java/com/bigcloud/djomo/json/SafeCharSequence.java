package com.bigcloud.djomo.json;

/**
 * A SafeCharSequence is a special kind of string representation; it must fit within a single buffer, and may only contain characters that do not require escaping.
 * 
 * The length() method, unlike a normal char sequence, may be approximate, as long as it represents a maximum number of required characters to represent the value.
 * 
 * The getChars method returns the end position of the target buffer, which may be equal to or less than dstBegin + length()
 * 
 * @author Alex Vigdor
 *
 */
public interface SafeCharSequence extends CharSequence {
	/*
	 * @return the first position in the destination buffer after the written characters
	 */
	int getChars(char[] dst, int dstBegin);
}
