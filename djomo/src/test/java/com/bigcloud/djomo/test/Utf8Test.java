/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.io.Utf8StreamReader;
import com.bigcloud.djomo.io.Utf8StreamSink;

public class Utf8Test {

	@Test
	public void testTwoByteWrite() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = new char[4096];
		sink.buffer(buf);
		Arrays.fill(buf, 'a');
		buf[4090] = 'é';
		sink.next(4096);
		String rt = baos.toString(StandardCharsets.UTF_8);
		Assert.assertEquals(rt.length(), 4096);
		Assert.assertEquals(rt.charAt(4090), 'é');
	}

	@Test
	public void testThreeByteWrite() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = new char[4096];
		sink.buffer(buf);
		Arrays.fill(buf, 'a');
		buf[4095] = '\u20ac';
		sink.next(4096);
		String rt = baos.toString(StandardCharsets.UTF_8);
		Assert.assertEquals(rt.length(), 4096);
		Assert.assertEquals(rt.charAt(4095), '\u20ac');
	}

	@Test
	public void testFourByteWrite() {
		String emojiStr = Character.toString(0x1F604);
		char[] emojiChars = emojiStr.toCharArray();
		Assert.assertEquals(emojiChars.length, 2);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = new char[4096];
		sink.buffer(buf);
		Arrays.fill(buf, 'a');
		buf[4094] = emojiChars[0];
		buf[4095] = emojiChars[1];
		sink.next(4096);
		String rt = baos.toString(StandardCharsets.UTF_8);
		Assert.assertEquals(rt.length(), 4096);
		Assert.assertEquals(rt.substring(4094), emojiStr);
	}

	@Test
	public void testFourByteOverflowWrite() {
		String emojiStr = Character.toString(0x1F604);
		char[] emojiChars = emojiStr.toCharArray();
		Assert.assertEquals(emojiChars.length, 2);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = new char[4096];
		sink.buffer(buf);
		Arrays.fill(buf, 'a');
		buf[4095] = emojiChars[0];
		sink.next(4096);
		buf[0] = emojiChars[1];
		sink.next(1);
		String rt = baos.toString(StandardCharsets.UTF_8);
		Assert.assertEquals(rt.length(), 4097);
		Assert.assertEquals(rt.substring(4095), emojiStr);
	}

	//
	// Sink: chars with bit 15 set (U+8000-U+FFFF, i.e. CJK, Hangul, BOM, and
	// surrogates). The old 3-byte discriminator, (c & 0xfffd8000) == 0, only fired
	// when bit 15 was clear, mis-routing all of these to the 4-byte branch, which
	// combined them with their neighbor as if they were surrogate pairs.
	//

	//
	// Sunk bytes are decoded through djomo's own reader: it round-trips unpaired
	// surrogates as-is, while the JDK decoder would replace them with U+FFFD.
	//

	private static String decodeBytes(byte[] data) throws IOException {
		char[] dest = new char[256];
		var out = new StringBuilder();
		Utf8StreamReader reader = new Utf8StreamReader(data);
		int n;
		while ((n = reader.read(dest)) > 0) {
			out.append(dest, 0, n);
		}
		return out.toString();
	}

	private static String sinkToString(String text, boolean last) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = text.toCharArray();
		sink.buffer(buf);
		if (last) {
			sink.last(buf.length);
		} else {
			sink.next(buf.length);
		}
		return decodeBytes(baos.toByteArray());
	}

	@Test
	public void testBit15CharMidBuffer() throws IOException {
		// U+8000 is a 3-byte char with bit 15 set; it must not swallow the 'a'.
		Assert.assertEquals(sinkToString("耀a", false), "耀a");
	}

	@Test
	public void testBit15CharAtBufferEnd() throws IOException {
		// A lone bit-15 char at the end of the chunk is written, not deferred to hi
		// (and then dropped or combined with the neighbor of the next chunk).
		Assert.assertEquals(sinkToString("가", false), "가");
	}

	@Test
	public void testBomWrite() throws IOException {
		Assert.assertEquals(sinkToString("\uFEFF", false), "\uFEFF");
	}

	@Test
	public void testBit15Run() throws IOException {
		Assert.assertEquals(sinkToString("耀가x", false), "耀가x");
	}

	@Test
	public void testBit15CharLast() throws IOException {
		Assert.assertEquals(sinkToString("가", true), "가");
	}

	@Test
	public void testUnpairedHighMidBuffer() throws IOException {
		// A lone high surrogate mid-buffer is 3 bytes UTF-8; it must not eat the 'a'.
		Assert.assertEquals(sinkToString("\uD800a", false), "\uD800a");
	}

	@Test
	public void testLoneLowSurrogate() throws IOException {
		Assert.assertEquals(sinkToString("\uDC00", false), "\uDC00");
	}

	@Test
	public void testUnpairedHighsAcrossCalls() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = new char[2];
		sink.buffer(buf);
		buf[0] = '\ud800';
		buf[1] = '\ud801';
		sink.next(2);
		buf[0] = 'a';
		buf[1] = 'b';
		sink.next(2);
		// The high deferred at the first chunk end must be emitted in order, before 'ab'.
		Assert.assertEquals(decodeBytes(baos.toByteArray()), "\ud800\ud801ab");
	}

	@Test
	public void testDeferredHighFlushedInOrder() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = new char[2];
		sink.buffer(buf);
		buf[0] = 'x';
		buf[1] = '\ud800';
		sink.next(2);
		buf[0] = 'y';
		buf[1] = 'z';
		sink.next(2);
		Assert.assertEquals(decodeBytes(baos.toByteArray()), "x\ud800yz");
	}

	@Test
	public void testPairSplitAcrossCalls() throws IOException {
		// A real supplementary pair split across chunks still combines (regression).
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] high = { '\uD83D' };
		sink.buffer(high);
		sink.next(1);
		char[] low = { '\uDE00' };
		sink.buffer(low);
		sink.next(1);
		Assert.assertEquals(decodeBytes(baos.toByteArray()), "\uD83D\uDE00");
	}

	@Test
	public void testLastFlushesDeferredHigh() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = { '\uD800' };
		sink.buffer(buf);
		sink.next(1);
		// The document ends with the deferred high; last() must emit it.
		sink.buffer(new char[0]);
		sink.last(0);
		Assert.assertEquals(decodeBytes(baos.toByteArray()), "\uD800");
	}

	@Test
	public void testSinkReaderRoundTrip() throws IOException {
		String sample = "a耀b가c\uFEFFd\u20AC\u00E9" + Character.toString(0x1F600) + "e";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = sample.toCharArray();
		sink.buffer(buf);
		sink.last(buf.length);
		Assert.assertEquals(decodeBytes(baos.toByteArray()), sample);
	}

	@Test
	public void testTwoByteRead() throws IOException {
		Utf8StreamReader reader = new Utf8StreamReader("é".getBytes(StandardCharsets.UTF_8));
		char[] dest = new char[1];
		int read = reader.read(dest);
		Assert.assertEquals(read, 1);
		Assert.assertEquals(dest[0], 'é');
	}

	@Test
	public void testThreeByteRead() throws IOException {
		String testStr = Character.toString(0x20ac);
		Utf8StreamReader reader = new Utf8StreamReader(testStr.getBytes(StandardCharsets.UTF_8));
		char[] dest = new char[1];
		int read = reader.read(dest);
		Assert.assertEquals(read, 1);
		Assert.assertEquals(dest[0], 0x20ac);
	}

	@Test
	public void testFourByteRead() throws IOException {
		String testStr = Character.toString(0x1F604);
		Utf8StreamReader reader = new Utf8StreamReader(testStr.getBytes(StandardCharsets.UTF_8));
		char[] dest = new char[2];
		int read = reader.read(dest);
		Assert.assertEquals(read, 2);
		Assert.assertEquals(new String(dest), testStr);
	}

	@Test
	public void testFourByteOverflowReadBytes() throws IOException {
		String testStr = Character.toString(0x1F604);
		for (int overflow = 1; overflow <= 3; overflow++) {
			char[] prefix = new char[8192 - overflow];
			Arrays.fill(prefix, 'a');
			String sample = (new String(prefix) + testStr);
			Utf8StreamReader reader = new Utf8StreamReader(sample.getBytes(StandardCharsets.UTF_8));
			char[] dest = new char[8192];
			StringBuilder builder = new StringBuilder();
			int c;
			while ((c = reader.read(dest)) != -1) {
				builder.append(dest, 0, c);
			}
			Assert.assertEquals(builder.toString().substring(8190), sample.substring(8190));
		}

	}

	@Test
	public void testFourByteOverflowReadStream() throws IOException {
		String testStr = Character.toString(0x1F604);
		for (int overflow = 1; overflow <= 3; overflow++) {
			char[] prefix = new char[8192 - overflow];
			Arrays.fill(prefix, 'a');
			String sample = (new String(prefix) + testStr);
			Utf8StreamReader reader = new Utf8StreamReader(
					new ByteArrayInputStream(sample.getBytes(StandardCharsets.UTF_8)));
			char[] dest = new char[8192];
			StringBuilder builder = new StringBuilder();
			int c;
			while ((c = reader.read(dest)) != -1) {
				builder.append(dest, 0, c);
			}
			Assert.assertEquals(builder.toString().substring(8190), sample.substring(8190));
		}

	}

	//
	// SWAR regression: the tests above fill uniform 'a', which cannot expose a
	// byte-ordering bug in the 8-char-wide SWAR pack/spread (a doubled or dropped
	// char among identical fill is invisible). These use distinct ASCII instead.
	//

	@Test
	public void testSwarPackDistinct() throws IOException {
		// 8 chars = exactly one SWAR window; distinct values pin the byte ordering.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utf8StreamSink sink = new Utf8StreamSink(baos);
		char[] buf = "abcdefgh".toCharArray();
		sink.buffer(buf);
		sink.next(8);
		byte[] out = baos.toByteArray();
		Assert.assertEquals(out.length, 8);
		Assert.assertEquals(new String(out, StandardCharsets.UTF_8), "abcdefgh");

		// 100 chars = 12 SWAR windows + a 4-char scalar tail.
		char[] wide = new char[100];
		for (int i = 0; i < 100; i++) {
			wide[i] = (char) ('a' + (i % 26));
		}
		ByteArrayOutputStream wideOut = new ByteArrayOutputStream();
		Utf8StreamSink wideSink = new Utf8StreamSink(wideOut);
		wideSink.buffer(wide);
		wideSink.next(100);
		Assert.assertEquals(decodeBytes(wideOut.toByteArray()), new String(wide));
	}

	@Test
	public void testSwarReaderDistinct() throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			sb.append((char) ('a' + (i % 26)));
		}
		String expected = sb.toString();
		byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
		// Odd cbuf sizes straddle the 16-byte and 8-byte SWAR windows.
		for (int size : new int[] { 1, 7, 8, 15, 16, 17, 24, 33 }) {
			Utf8StreamReader reader = new Utf8StreamReader(bytes);
			char[] dest = new char[size];
			StringBuilder out = new StringBuilder();
			int n;
			while ((n = reader.read(dest, 0, size)) > 0) {
				out.append(dest, 0, n);
			}
			Assert.assertEquals(out.toString(), expected, "cbuf size " + size);
		}
	}
}
