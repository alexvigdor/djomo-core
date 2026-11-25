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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.api.Filters;
import com.bigcloud.djomo.internal.ByteBufferPool;
import com.bigcloud.djomo.internal.CharBufferPool;
import com.bigcloud.djomo.internal.CharBufferPool.CharPair;
import com.bigcloud.djomo.io.CharArraySink;
import com.bigcloud.djomo.io.Utf8StreamReader;
import com.bigcloud.djomo.io.Utf8StreamSink;
import com.bigcloud.djomo.json.JsonParser;
import com.bigcloud.djomo.json.JsonWriter;

import static com.bigcloud.djomo.test.PoolTestSupport.onVirtual;

/**
 * End-to-end proof that the shared buffer pools support <b>re-entrant</b> JSON calls on one
 * thread and <b>concurrent</b> use from many virtual threads.
 *
 * <p>
 * The pre-pool design gave every operation on a thread the same buffer, so a JSON write or
 * read nested inside another one (the exact "reentrant json call" this class exercises)
 * clobbered the outer operation's buffer. The pools return a *different* buffer to every
 * re-entrant acquire on the same thread, and route virtual threads through the MPMC rings.
 *
 * <p>
 * Each scenario asserts <i>exact</i> output: with a shared buffer the outer operation's
 * pending bytes are overwritten by the inner one, which shows up as corrupted output.
 * The {@code poolCount()} scenarios run on virtual threads (see {@link PoolTestSupport}),
 * where every acquire/release goes through the rings, and measure the pool relative to a
 * count taken on the same virtual thread just before the acquire, because the rings may
 * hold residue left by other tests (the pools are process-wide singletons).
 */
public class ReentrantJsonTest {

	private static final Models MODELS = new Models();
	private static final Json JSON = new Json(MODELS);
	private static final String UTF8_DOC = "a".repeat(100_000);

	//
	// Nested writers: an outer writer still holding pending buffer content, with a full
	// JSON write nested inside it on the same thread. The inner write's 100k string forces
	// many full-buffer flushes through a buffer that (pre-pool) was the outer writer's own.
	//

	@Test
	public void testNestedWriterCharSink() {
		var sink = new CharArraySink();
		var outer = new JsonWriter(MODELS, sink);
		outer.visitInt(1);
		String inner = JSON.toString(Map.of("big", UTF8_DOC));
		assertEquals(inner, "{\"big\":\"" + UTF8_DOC + "\"}");
		outer.visitInt(2);
		outer.close();
		assertEquals(sink.toString(), "12", "the inner write must not have clobbered the outer writer's buffer");
	}

	@Test
	public void testNestedWriterCharSinkVirtual() {
		onVirtual(() -> {
			var sink = new CharArraySink();
			var outer = new JsonWriter(MODELS, sink);
			outer.visitInt(1);
			String inner = JSON.toString(Map.of("big", UTF8_DOC));
			assertEquals(inner, "{\"big\":\"" + UTF8_DOC + "\"}");
			outer.visitInt(2);
			outer.close();
			assertEquals(sink.toString(), "12");
			return null;
		});
	}

	@Test
	public void testNestedWriterUtf8Sink() {
		var out1 = new ByteArrayOutputStream();
		var out2 = new ByteArrayOutputStream();
		var outer = new JsonWriter(MODELS, new Utf8StreamSink(out1));
		outer.visitInt(1);
		JSON.write(Map.of("big", UTF8_DOC), out2);
		outer.visitInt(2);
		outer.close();
		assertEquals(out1.toString(StandardCharsets.UTF_8), "12");
		assertEquals(out2.toString(StandardCharsets.UTF_8), "{\"big\":\"" + UTF8_DOC + "\"}");
	}

	@Test
	public void testNestedWriterUtf8SinkVirtual() {
		onVirtual(() -> {
			var out1 = new ByteArrayOutputStream();
			var out2 = new ByteArrayOutputStream();
			var outer = new JsonWriter(MODELS, new Utf8StreamSink(out1));
			outer.visitInt(1);
			JSON.write(Map.of("big", UTF8_DOC), out2);
			outer.visitInt(2);
			outer.close();
			assertEquals(out1.toString(StandardCharsets.UTF_8), "12");
			assertEquals(out2.toString(StandardCharsets.UTF_8), "{\"big\":\"" + UTF8_DOC + "\"}");
			return null;
		});
	}

	//
	// Nested readers: r1 has a partially consumed window in its pooled buffer, then r2
	// (same thread) refills its own buffer to EOF, then r1 resumes. Pre-pool, both readers
	// shared one buffer, so r1's pending window bytes were r2's data.
	//

	@Test
	public void testNestedReaderInterleaved() throws Exception {
		byte[] stream1 = "a".repeat(5_000).getBytes(StandardCharsets.UTF_8);
		byte[] stream2 = "b".repeat(5_000).getBytes(StandardCharsets.UTF_8);
		var r1 = new Utf8StreamReader(new ByteArrayInputStream(stream1));
		var r2 = new Utf8StreamReader(new ByteArrayInputStream(stream2));
		try {
			char[] d1 = new char[1_000];
			char[] d2 = new char[1_000];
			assertEquals(r1.read(d1), 1_000);
			var sb1 = new StringBuilder(5_000);
			sb1.append(d1, 0, 1_000);
			// r1's window still has 4_000 unconsumed bytes; r2 now refills its buffer to EOF
			var sb2 = new StringBuilder(5_000);
			int n;
			while ((n = r2.read(d2)) > 0) {
				sb2.append(d2, 0, n);
			}
			assertEquals(sb2.toString(), "b".repeat(5_000));
			while ((n = r1.read(d1)) > 0) {
				sb1.append(d1, 0, n);
			}
			assertEquals(sb1.toString(), "a".repeat(5_000), "r2's refills must not have clobbered r1's pending window");
		} finally {
			r1.close();
			r2.close();
		}
	}

	@Test
	public void testNestedReaderInterleavedVirtual() {
		onVirtual(() -> {
			byte[] stream1 = "a".repeat(5_000).getBytes(StandardCharsets.UTF_8);
			byte[] stream2 = "b".repeat(5_000).getBytes(StandardCharsets.UTF_8);
			var r1 = new Utf8StreamReader(new ByteArrayInputStream(stream1));
			var r2 = new Utf8StreamReader(new ByteArrayInputStream(stream2));
			try {
				char[] d1 = new char[1_000];
				char[] d2 = new char[1_000];
				assertEquals(r1.read(d1), 1_000);
				var sb1 = new StringBuilder(5_000);
				sb1.append(d1, 0, 1_000);
				var sb2 = new StringBuilder(5_000);
				int n;
				while ((n = r2.read(d2)) > 0) {
					sb2.append(d2, 0, n);
				}
				assertEquals(sb2.toString(), "b".repeat(5_000));
				while ((n = r1.read(d1)) > 0) {
					sb1.append(d1, 0, n);
				}
				assertEquals(sb1.toString(), "a".repeat(5_000));
			} catch (Exception e) {
				throw new AssertionError("nested reader scenario failed", e);
			} finally {
				closeQuietly(r1);
				closeQuietly(r2);
			}
			return null;
		});
	}

	//
	// Two parsers open at once on the same thread, each with a string large enough to use
	// both pooled buffers (input window + overflow).
	//

	@Test
	public void testTwoOpenParsersBothParse() throws Exception {
		var p1 = new JsonParser(MODELS, new StringReader("{\"a\":\"" + "x".repeat(9_000) + "\"}"));
		var p2 = new JsonParser(MODELS, new StringReader("{\"b\":\"" + "y".repeat(3_000) + "\"}"));
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> m2 = (Map<String, Object>) p2.parse();
			assertEquals(m2.get("b").toString(), "y".repeat(3_000));
			p2.close();
			@SuppressWarnings("unchecked")
			Map<String, Object> m1 = (Map<String, Object>) p1.parse();
			assertEquals(m1.get("a").toString(), "x".repeat(9_000));
		} finally {
			p1.close();
		}
	}

	@Test
	public void testTwoOpenParsersBothParseVirtual() {
		onVirtual(() -> {
			var p1 = new JsonParser(MODELS, new StringReader("{\"a\":\"" + "x".repeat(9_000) + "\"}"));
			var p2 = new JsonParser(MODELS, new StringReader("{\"b\":\"" + "y".repeat(3_000) + "\"}"));
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> m2 = (Map<String, Object>) p2.parse();
				assertEquals(m2.get("b").toString(), "y".repeat(3_000));
				p2.close();
				@SuppressWarnings("unchecked")
				Map<String, Object> m1 = (Map<String, Object>) p1.parse();
				assertEquals(m1.get("a").toString(), "x".repeat(9_000));
			} finally {
				p1.close();
			}
			return null;
		});
	}

	//
	// The sharpest parser-side proof: a parser filter makes a full nested json.read()
	// *mid-parse*, while the outer parser still has a pending input window (the 12k string
	// forces a refill). Pre-pool, the inner parser refilled the shared buffers and the
	// outer parse then saw the inner document.
	//

	@Test
	public void testReentrantParserCallback() throws Exception {
		String a = "a".repeat(12_000); // forces the parser to refill its input buffer
		String b = "b".repeat(8_000); // the document fills the inner parser's pooled input window
		String outerDoc = "[\"" + a + "\"]";
		String innerDoc = "{\"k\":\"" + b + "\"}";
		var fired = new AtomicBoolean(false);

		List<?> result = JSON.read(new StringReader(outerDoc), List.class,
				Filters.parseString(parser -> {
					CharSequence cs = parser.parseString();
					if (fired.compareAndSet(false, true) && cs.length() > 1_000) {
						try {
							@SuppressWarnings("unchecked")
							Map<?, ?> inner = (Map<?, ?>) JSON.read(new StringReader(innerDoc), Map.class);
							assertEquals(inner.size(), 1);
							assertEquals(inner.get("k").toString(), b);
						} catch (IOException e) {
							throw new AssertionError("nested read inside the re-entrant parse failed", e);
						}
					}
					return cs;
				}));

		assertTrue(fired.get(), "the re-entrant read inside the parse must have happened");
		assertEquals(result.size(), 1);
		assertEquals(result.get(0).toString(), a, "the nested read must not have clobbered the outer parser's buffers");
	}

	@Test
	public void testReentrantParserCallbackVirtual() {
		onVirtual(() -> {
			String a = "a".repeat(12_000);
			String b = "b".repeat(8_000);
			String outerDoc = "[\"" + a + "\"]";
			String innerDoc = "{\"k\":\"" + b + "\"}";
			var fired = new AtomicBoolean(false);

			List<?> result;
			try {
				result = JSON.read(new StringReader(outerDoc), List.class,
						Filters.parseString(parser -> {
							CharSequence cs = parser.parseString();
							if (fired.compareAndSet(false, true) && cs.length() > 1_000) {
								try {
									@SuppressWarnings("unchecked")
									Map<?, ?> inner = (Map<?, ?>) JSON.read(new StringReader(innerDoc), Map.class);
									assertEquals(inner.size(), 1);
									assertEquals(inner.get("k").toString(), b);
								} catch (IOException e) {
									throw new AssertionError("nested read inside the re-entrant parse failed", e);
								}
							}
							return cs;
						}));
			} catch (Exception e) {
				throw new AssertionError("re-entrant parse failed", e);
			}
			assertTrue(fired.get());
			assertEquals(result.size(), 1);
			assertEquals(result.get(0).toString(), a);
			return null;
		});
	}

	//
	// The CharArraySink fix, in isolation: the sink must copy the final chunk out of the
	// (pooled) buffer in last(); a pre-fix sink kept a lazy reference and read the buffer
	// in toString(), which the writer's close() had already returned to the pool.
	//

	@Test
	public void testCharArraySinkCopiesFinalChunkBeforeBufferRecycled() {
		String docA = "The quick brown fox jumps over the lazy dog. ".repeat(25);
		CharPair pair = CharBufferPool.acquire();
		var sink = new CharArraySink();
		sink.buffer(pair.io);
		docA.getChars(0, docA.length(), pair.io, 0);
		sink.last(docA.length()); // a writer's close() does this, then...
		CharBufferPool.release(pair); // ...returns the pair to the pool
		// the recycled buffer may now be owned by another consumer; stomp on it
		Arrays.fill(pair.io, 'X');
		assertEquals(sink.toString(), docA, "the sink must never read the buffer after last()");
	}

	//
	// Close idempotency with exact pool accounting (virtual thread; counts measured
	// relative to the ctor because the pool may hold residue).
	//

	@Test
	public void testWriterCloseReleasesPairOnce() {
		onVirtual(() -> {
			int before = CharBufferPool.poolCount();
			var sink = new CharArraySink();
			var writer = new JsonWriter(MODELS, sink); // the ctor acquires the pair
			int afterCtor = CharBufferPool.poolCount();
			assertTrue(afterCtor >= before - 1 && afterCtor <= before, "the ctor's acquire must remove at most one pair");
			writer.visit(ImmutableModel.builder().name("foo").enabled(true).count(5).build());
			writer.close();
			assertEquals(CharBufferPool.poolCount(), afterCtor + 1, "close must return the writer's pair to the pool");
			assertEquals(sink.toString(), "{\"count\":5,\"enabled\":true,\"name\":\"foo\"}");
			writer.close(); // idempotent
			assertEquals(CharBufferPool.poolCount(), afterCtor + 1, "a second close must not release the pair twice");
		});
	}

	@Test
	public void testParserCloseReleasesPairOnce() {
		onVirtual(() -> {
			int before = CharBufferPool.poolCount();
			var parser = new JsonParser(MODELS, new StringReader("{\"count\":5,\"enabled\":true,\"name\":\"foo\"}"));
			int afterCtor = CharBufferPool.poolCount();
			assertTrue(afterCtor >= before - 1 && afterCtor <= before, "the ctor's acquire must remove at most one pair");
			Object result = parser.parse();
			if (!(result instanceof Map<?, ?> m)) {
				throw new AssertionError("the document must parse to a map");
			}
			assertEquals(m.size(), 3);
			assertEquals(m.get("count"), 5);
			assertEquals(m.get("enabled"), true);
			assertEquals(m.get("name"), "foo");
			parser.close();
			assertEquals(CharBufferPool.poolCount(), afterCtor + 1, "close must return the parser's pair to the pool");
			parser.close(); // idempotent
			assertEquals(CharBufferPool.poolCount(), afterCtor + 1, "a second close must not release the pair twice");
			});
	}

	//
	// Utf8StreamReader.close() must return the pooled buffer even when the stream has not
	// reached EOF (the common case: the document ends inside the reader's window), and a
	// double close must not release it twice.
	//

	@Test
	public void testReaderCloseReturnsBuffer() {
		onVirtual(() -> {
			int before = ByteBufferPool.poolCount();
			byte[] data = "a".repeat(5_000).getBytes(StandardCharsets.UTF_8);
			var reader = new Utf8StreamReader(new ByteArrayInputStream(data));
			int afterCtor = ByteBufferPool.poolCount();
			assertTrue(afterCtor >= before - 1 && afterCtor <= before, "the ctor's acquire must remove at most one buffer");
			try {
				char[] d = new char[100];
				assertEquals(reader.read(d), 100); // partial read: window pending, buffer in use
				reader.close();
				assertEquals(ByteBufferPool.poolCount(), afterCtor + 1, "close must return the buffer even when the stream is not at EOF");
				assertEquals(reader.read(d), -1, "a read after close must not touch the recycled buffer");
				reader.close(); // idempotent
				assertEquals(ByteBufferPool.poolCount(), afterCtor + 1, "a second close must not release the buffer twice");
			} catch (Exception e) {
				throw new AssertionError("reader close scenario failed", e);
			} finally {
				closeQuietly(reader);
			}
			return null;
		});
	}

	//
	// Utf8StreamSink.last() must release its pooled buffer exactly once: a repeated last()
	// pre-fix both re-emitted the tail and put the same buffer in the pool twice (two
	// concurrent owners).
	//

	@Test
	public void testSinkDoubleLastEmitsOnce() {
		onVirtual(() -> {
			int before = ByteBufferPool.poolCount();
			String doc = "a".repeat(10_000);
			var out = new ByteArrayOutputStream();
			var sink = new Utf8StreamSink(out); // the field initializer acquires the buffer
			int afterCtor = ByteBufferPool.poolCount();
			assertTrue(afterCtor >= before - 1 && afterCtor <= before, "the initializer's acquire must remove at most one buffer");
			char[] chars = doc.toCharArray();
			sink.buffer(chars);
			sink.next(5_000);
			sink.last(5_000);
			assertEquals(ByteBufferPool.poolCount(), afterCtor + 1, "last() must return the buffer to the pool");
			sink.last(5_000); // the pre-fix double-release
			assertEquals(ByteBufferPool.poolCount(), afterCtor + 1, "a second last() must not release the buffer twice");
			assertEquals(new String(out.toByteArray(), StandardCharsets.UTF_8), doc, "the document must appear exactly once");
			});
	}

	//
	// Concurrent virtual threads sharing one Json: every write and read goes through the
	// rings. All outputs must be byte-exact, and no pair may ever be released twice.
	//

	@Test
	public void testConcurrentVirtualThreadsJsonRoundTrip() throws InterruptedException {
		var doc = Map.of("s", "z".repeat(2_000));
		String expected = "{\"s\":\"" + "z".repeat(2_000) + "\"}";
		int baselineChar = CharBufferPool.poolCount();
		int baselineByte = ByteBufferPool.poolCount();

		final int threads = 16;
		final int iterations = 50;
		var done = new CountDownLatch(threads);
		var errors = new AtomicInteger();
		for (int i = 0; i < threads; i++) {
			Thread v = Thread.ofVirtual().unstarted(() -> {
				try {
					for (int k = 0; k < iterations; k++) {
						String out = JSON.toString(doc);
						if (!expected.equals(out)) {
							errors.incrementAndGet();
							return;
						}
						@SuppressWarnings("unchecked")
						Map<?, ?> back = (Map<?, ?>) JSON.fromString(out, Map.class);
						if (!"z".repeat(2_000).equals(String.valueOf(back.get("s")))) {
							errors.incrementAndGet();
							return;
						}
					}
				} catch (Throwable t) {
					errors.incrementAndGet();
				} finally {
					done.countDown();
				}
			});
			v.start();
		}
		assertTrue(done.await(180, TimeUnit.SECONDS), "virtual threads did not finish in time");
		assertEquals(errors.get(), 0, "all round-trips must be byte-exact");
		// every released pair stays in the rings (or is dropped only if a ring is full),
		// so the pool can only grow by the fresh allocations the workers had to make
		int afterChar = CharBufferPool.poolCount();
		int maxAcquires = threads * iterations * 2; // one writer pair + one parser pair per iteration
		assertTrue(afterChar <= baselineChar + maxAcquires,
				"poolCount " + afterChar + " exceeds baseline " + baselineChar + " + " + maxAcquires
						+ " acquires; a double release or a ring accounting bug");
		assertEquals(ByteBufferPool.poolCount(), baselineByte, "the string-based round trip must not touch the byte pool");
	}

	private static void closeQuietly(Closeable c) {
		try {
			c.close();
		} catch (IOException e) {
			throw new AssertionError("close failed", e);
		}
	}
}
