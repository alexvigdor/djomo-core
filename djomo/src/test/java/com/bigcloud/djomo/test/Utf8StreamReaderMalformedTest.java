/*******************************************************************************
 * Copyright 2026 Alex Vigdor
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
package com.bigcloud.djomo.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.io.Utf8StreamReader;

/**
 * Resilience probes for {@link Utf8StreamReader} against malformed UTF-8, plus
 * regression tests for a happy-path defect that was fixed by replacing the
 * reader's supplementary-pair discriminator, {@code (code & 0xfffd8000) != 0}, with a
 * single comparison, {@code code > 0xFFFF}.
 *
 * <p>The old mask reduces to a plain bit-15 test for the 16-bit codes produced by 2-
 * and 3-byte sequences. Consequently every 3-byte character at or above U+0800 (CJK,
 * Hangul, BOM, ...) was expanded into a bogus surrogate pair whose high member is not
 * even a high surrogate. The tests marked below assert the correct single-char output
 * (and a lone-surrogate char for sequences decoding into the surrogate window).
 *
 * <p><b>Characterization (green):</b> the reader never throws on malformed bytes and
 * never emits U+FFFD; it fabricates characters. These tests pin the current lenient
 * behavior so that any future hardening is a deliberate, visible change.
 */
public class Utf8StreamReaderMalformedTest {

	private static final Charset UTF_8 = StandardCharsets.UTF_8;

	private static String readAll(Reader reader) throws IOException {
		char[] dest = new char[8192];
		var out = new StringBuilder();
		int n;
		while ((n = reader.read(dest)) > 0) {
			out.append(dest, 0, n);
		}
		return out.toString();
	}

	private static String readAll(byte[] data) throws IOException {
		return readAll(new Utf8StreamReader(data));
	}

	private static String readAllStream(byte[] data) throws IOException {
		return readAll(new Utf8StreamReader(new ByteArrayInputStream(data)));
	}

	private static String hex(byte[] data) {
		var sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X ", b & 0xFF));
		}
		return sb.toString().trim();
	}

	//
	// Correct happy-path decoding (green): pins the behavior the bug must preserve.
	//

	@Test
	public void testBoundaryCharsDecodeCorrectly() throws IOException {
		// 2-byte and 3-byte chars below U+0800 decode correctly today (bit 15 clear).
		Assert.assertEquals(readAll("é".getBytes(UTF_8)), "é");
		byte[] u7fff = { (byte) 0xE7, (byte) 0xBF, (byte) 0xBF }; // 3-byte char just below the bug boundary
		Assert.assertEquals(readAll(u7fff), Character.toString(0x7FFF));
		Assert.assertEquals(readAllStream(u7fff), Character.toString(0x7FFF));
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xE0, (byte) 0xA0, (byte) 0x80 }), Character.toString(0x0800));
		Assert.assertEquals(readAll("一".getBytes(UTF_8)), "一"); // U+4E00, below the boundary
	}

	@Test
	public void testFourByteSupplementaryDecodesCorrectly() throws IOException {
		// The 4-byte path is unaffected by the bit-15 bug: supplementary codepoints
		// always carry a higher bit that is in the mask.
		Assert.assertEquals(readAll("😀".getBytes(UTF_8)), "😀"); // U+1F600
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x80 }), Character.toString(0x10400));
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xF4, (byte) 0x8F, (byte) 0xBF, (byte) 0xBF }), Character.toString(0x10FFFF));
	}

	@Test
	public void testSupplementaryPairSplitAtCharBufferBoundary() throws IOException {
		// A 4-byte sequence whose pair does not fit the char buffer defers the low
		// surrogate and emits it on the next read.
		byte[] emoji = "😀".getBytes(UTF_8);
		Reader reader = new Utf8StreamReader(emoji);
		char[] d1 = new char[1];
		char[] d2 = new char[2];
		Assert.assertEquals(reader.read(d1, 0, 1), 1);
		Assert.assertEquals(d1[0], 0xD83D);
		Assert.assertEquals(reader.read(d2, 0, 2), 1);
		Assert.assertEquals(d2[0], 0xDE00);
		Assert.assertEquals(reader.read(d2), -1);
		// and when the pair fits, both chars arrive at once.
		Reader r2 = new Utf8StreamReader(new ByteArrayInputStream(emoji));
		Assert.assertEquals(r2.read(d2, 0, 2), 2);
		Assert.assertEquals(new String(d2, 0, 2), "😀");
		Assert.assertEquals(r2.read(d2), -1);
	}

	@Test
	public void testSequenceStraddlingStreamWindows() throws IOException {
		// Feeding the stream two bytes per window forces a 3-byte sequence to span
		// read() calls, exercising the partial-sequence state machine.
		byte[] euro = "€".getBytes(UTF_8); // E2 82 AC
		Reader reader = new Utf8StreamReader(new ByteArrayInputStream(euro) {
			@Override
			public int read(byte[] b, int off, int len) {
				return super.read(b, off, Math.min(len, 2));
			}
		});
		char[] dest = new char[8];
		// First window (E2 82) completes nothing: current behavior returns 0.
		Assert.assertEquals(reader.read(dest), 0);
		// Second window (AC) completes the sequence.
		Assert.assertEquals(new String(dest, 0, reader.read(dest)), "€");
		Assert.assertEquals(reader.read(dest), -1);
	}

	//
	// Malformed input: the reader is consistently lenient (green characterization).
	//

	@Test
	public void testTruncatedSequenceAtEof() throws IOException {
		// A multi-byte sequence cut off at EOF is silently dropped: preceding
		// characters are returned, EOF (-1) is reported, and -1 is sticky.
		checkTruncatedAtEof(new byte[]{ 'a', 'b', (byte) 0xC3 }, "ab");
		checkTruncatedAtEof(new byte[]{ 'a', 'b', (byte) 0xE0, (byte) 0xA0 }, "ab");
		checkTruncatedAtEof(new byte[]{ 'a', 'b', (byte) 0xF0, (byte) 0x9F, (byte) 0x8C }, "ab");
		checkTruncatedAtEof(new byte[]{ (byte) 0xE0, (byte) 0xA0 }, "");
		checkTruncatedAtEof(new byte[]{ (byte) 0xF0, (byte) 0x9F, (byte) 0x8C }, "");
	}

	private static void checkTruncatedAtEof(byte[] data, String expected) throws IOException {
		char[] dest = new char[16];
		Reader bytewise = new Utf8StreamReader(data);
		int n = bytewise.read(dest);
		Assert.assertEquals(n, expected.length(), "byte[] ctor, input=" + hex(data));
		if (n > 0) {
			Assert.assertEquals(new String(dest, 0, n), expected, "byte[] ctor, input=" + hex(data));
		}
		Assert.assertEquals(bytewise.read(dest), -1, "byte[] ctor EOF, input=" + hex(data));
		Assert.assertEquals(bytewise.read(dest), -1, "byte[] ctor EOF sticky, input=" + hex(data));
		Reader streaming = new Utf8StreamReader(new ByteArrayInputStream(data));
		n = streaming.read(dest);
		Assert.assertEquals(n, expected.length(), "stream ctor, input=" + hex(data));
		if (n > 0) {
			Assert.assertEquals(new String(dest, 0, n), expected, "stream ctor, input=" + hex(data));
		}
		Assert.assertEquals(streaming.read(dest), -1, "stream ctor EOF, input=" + hex(data));
		Assert.assertEquals(streaming.read(dest), -1, "stream ctor EOF sticky, input=" + hex(data));
	}

	@Test
	public void testIncompleteInputReturnsZeroThenEof() throws IOException {
		// Current behavior: input that is exactly one incomplete sequence returns 0
		// on the first read (a Reader-contract wart: 0 with len > 0) and then -1.
		char[] dest = new char[16];
		Reader reader = new Utf8StreamReader(new byte[]{ (byte) 0xC3 });
		Assert.assertEquals(reader.read(dest), 0);
		Assert.assertEquals(reader.read(dest), -1);
		Reader streaming = new Utf8StreamReader(new ByteArrayInputStream(new byte[]{ (byte) 0xF0 }));
		Assert.assertEquals(streaming.read(dest), 0);
		Assert.assertEquals(streaming.read(dest), -1);
	}

	@Test
	public void testStrayContinuationByteStartsFakeSequence() throws IOException {
		// A stray continuation byte (0x80-0xBF) is treated as a 4-byte lead and eats
		// the next three bytes, absorbing valid ASCII into a single fabricated pair.
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xA9, 'a', 'b', 'c' }), "\uD946\uDCA3");
	}

	@Test
	public void testStrayContinuationByteTruncated() throws IOException {
		// With fewer than three followers, the fake sequence is cut off at EOF:
		// preceding characters are returned, then 0, then sticky -1.
		char[] dest = new char[16];
		Reader reader = new Utf8StreamReader(new byte[]{ (byte) 0xA9, 'a' });
		Assert.assertEquals(reader.read(dest), 0);
		Assert.assertEquals(reader.read(dest), -1);
		Reader streaming = new Utf8StreamReader(new ByteArrayInputStream(new byte[]{ 'a', 'b', (byte) 0xA9 }));
		int n = streaming.read(dest);
		Assert.assertEquals(n, 2);
		Assert.assertEquals(new String(dest, 0, n), "ab");
		Assert.assertEquals(streaming.read(dest), -1);
	}

	@Test
	public void testInvalidContinuationByteAbsorbed() throws IOException {
		// A non-continuation byte inside a sequence is masked to its low 6 bits and
		// folded into the codepoint, fabricating a single character:
		// C3 41 yields 0x03 << 6 | (0x41 & 0x3F) = U+00C1.
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xC3, 0x41, 'x' }), "\u00C1x");
	}

	@Test
	public void testOverlongEncodingsAccepted() throws IOException {
		// Overlong encodings of NUL decode to U+0000 rather than being rejected.
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xC0, (byte) 0x80 }), "\u0000");
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xE0, (byte) 0x80, (byte) 0x80 }), "\u0000");
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80 }), "\u0000");
		Assert.assertEquals(readAllStream(new byte[]{ (byte) 0xE0, (byte) 0x80, (byte) 0x80, 'x' }), "\u0000x");
	}

	@Test
	public void testOutOfRangeCodepointsFabricatePairs() throws IOException {
		// 4-byte sequences decoding above U+10FFFF fabricate a surrogate pair for
		// the out-of-range codepoint rather than emitting U+FFFD.
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xF5, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }), "\uDDBF\uDFFF");
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xFF, 'a', 'b', 'c' }), "\uDF46\uDCA3");
	}

	//
	// Regression: the bit-15 discriminator mis-decoded these; the tests assert the
	// correct single-char output.
	//

	@Test
	public void testThreeByteAboveU8000DecodesToSingleChar() throws IOException {
		// Regression: the bit-15 discriminator expanded every 3-byte char at or above
		// U+0800 into a bogus pair (U+8000 decoded as U+D7E0 U+DC00; the high member
		// is not even a high surrogate). Correct behavior: a single char.
		byte[] u8000 = { (byte) 0xE8, (byte) 0x80, (byte) 0x80 };
		Assert.assertEquals(readAll(u8000), Character.toString(0x8000));
		Assert.assertEquals(readAllStream(u8000), Character.toString(0x8000));
	}

	@Test
	public void testThreeByteHangulDecodesToSingleChar() throws IOException {
		// Regression: Hangul syllables U+AC00-U+D7AF are 3-byte sequences above
		// U+0800; U+AC00 decoded as U+D7EB U+DC00.
		Assert.assertEquals(readAll("가".getBytes(UTF_8)), "가");
	}

	@Test
	public void testBomDecodesToUFEFF() throws IOException {
		// Regression: the BOM (U+FEFF) is a 3-byte char above U+0800; it decoded as
		// U+D7FF U+DEFF.
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }), Character.toString(0xFEFF));
	}

	@Test
	public void testBomMidStream() throws IOException {
		// Regression: a mid-stream BOM used to corrupt the surrounding text.
		Assert.assertEquals(readAll(new byte[]{ 'a', (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'b' }), "a\uFEFFb");
	}

	@Test
	public void testSurrogateWindowDecodesToLoneSurrogate() throws IOException {
		// Regression: a 3-byte sequence decoding into the surrogate window was
		// expanded into a bogus pair (U+D800 decoded as U+D7F6 U+DC00). Lenient
		// behavior: emit the lone surrogate char as-is, consistent with String.
		Assert.assertEquals(readAll(new byte[]{ (byte) 0xED, (byte) 0xA0, (byte) 0x80 }), "\uD800");
	}

}
