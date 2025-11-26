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
}
