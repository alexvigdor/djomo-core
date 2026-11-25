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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.bigcloud.djomo.internal.ByteBufferPool;

import static com.bigcloud.djomo.test.PoolTestSupport.onVirtual;

/**
 * Tests for the shared {@link ByteBufferPool}: the per-thread cache for platform threads,
 * the ring-based branches used by virtual threads (which bypass the cache entirely), and
 * the Vyukov MPMC ring under mixed concurrent load.
 *
 * <p>
 * Exact {@link ByteBufferPool#poolCount()} assertions are only deterministic on virtual
 * threads: buffers parked in a live platform thread's per-thread cell are invisible to the
 * ring accounting, while virtual threads always route acquire/release through the rings.
 * The rings are a process-wide cache shared by all tests (surefire runs the suite in a
 * single fork, sequentially), so tests measure counts relative to a reading taken on the
 * same virtual thread just before each acquire, and pin identity only when the pool is
 * empty.
 *
 * <p>
 * The stress test fingerprints every byte of every acquired buffer with a worker-unique
 * pattern and verifies it before release; a ring protocol error or a double release that
 * hands the same buffer to two concurrent owners corrupts the pattern and fails a verify.
 */
public class ByteBufferPoolTest {

	@Test
	public void testAcquiredBufferSize() {
		byte[] buffer = ByteBufferPool.acquire();
		try {
			assertEquals(buffer.length, ByteBufferPool.BUFFER_SIZE);
		} finally {
			ByteBufferPool.release(buffer);
		}
	}

	@Test
	public void testReleaseNullIsNoOp() {
		int before = ByteBufferPool.poolCount();
		ByteBufferPool.release(null);
		assertEquals(ByteBufferPool.poolCount(), before);
	}

	/**
	 * A buffer of the wrong size is not part of the pool contract and must not enter the
	 * rings (it would be handed out as an 8192-byte buffer to a future consumer).
	 */
	@Test
	public void testWrongSizeReleaseIsIgnored() {
		int before = ByteBufferPool.poolCount();
		onVirtual(() -> {
			ByteBufferPool.release(new byte[100]);
			assertEquals(ByteBufferPool.poolCount(), before, "a wrong-size buffer must not enter the pool");
		});
	}

	/**
	 * A buffer released by a platform thread parks in that thread's per-thread cell, so the
	 * next acquire on the same thread gets the very same buffer back (deterministic).
	 */
	@Test
	public void testPlatformCellRoundTrip() {
		byte[] first = ByteBufferPool.acquire();
		ByteBufferPool.release(first);
		byte[] second = ByteBufferPool.acquire();
		try {
			assertSame(first, second, "a buffer released by a platform thread must be returned by the next acquire on that thread");
		} finally {
			ByteBufferPool.release(second);
		}
	}

	/**
	 * Reentrancy guarantee for the same thread: the per-thread cell holds at most one
	 * buffer, so a re-entrant acquire while one is in use always gets a *different*
	 * buffer. This is what lets a sink (or reader) nested inside another on the same
	 * thread work without clobbering the outer owner's buffer.
	 */
	@Test
	public void testReentrantAcquireIsDistinct() {
		byte[] first = ByteBufferPool.acquire();
		byte[] second = ByteBufferPool.acquire();
		try {
			assertNotSame(first, second, "re-entrant acquire on the same thread must return a distinct buffer");
		} finally {
			ByteBufferPool.release(first);
			ByteBufferPool.release(second);
		}
	}

	/**
	 * Virtual threads never touch the per-thread cell: every acquire and release goes
	 * through the rings. A ring acquire scans two stripes, so whether it hits a pooled
	 * buffer depends on residue left by other tests; the exact accounting is therefore
	 * made relative to a count taken on the same virtual thread right before each
	 * acquire, and the fully deterministic case (an empty pool) pins identity.
	 */
	@Test
	public void testVirtualThreadBranchExactAccounting() {
		onVirtual(() -> {
			int before = ByteBufferPool.poolCount();
			byte[] a = ByteBufferPool.acquire();
			int afterA = ByteBufferPool.poolCount();
			assertTrue(afterA >= before - 1 && afterA <= before,
					"a single acquire must remove at most one buffer from the rings");
			ByteBufferPool.release(a);
			assertEquals(ByteBufferPool.poolCount(), afterA + 1, "a VT release must land in a ring");
			if (before == 0) {
				assertEquals(afterA, 0, "a fresh allocation must not change the pool count");
				byte[] b = ByteBufferPool.acquire();
				assertEquals(ByteBufferPool.poolCount(), 0,
						"the released buffer is the pool's only buffer, so this acquire must take it from a ring");
				assertSame(a, b, "a buffer released to an empty pool must come straight back to the same thread");
				ByteBufferPool.release(b);
				assertEquals(ByteBufferPool.poolCount(), 1);
			}
			byte[] c = ByteBufferPool.acquire();
			byte[] d = ByteBufferPool.acquire();
			assertNotSame(c, d, "a re-entrant acquire on the same VT must return distinct buffers");
			int afterBoth = ByteBufferPool.poolCount();
			ByteBufferPool.release(c);
			ByteBufferPool.release(d);
			assertEquals(ByteBufferPool.poolCount(), afterBoth + 2, "each release must add exactly one buffer");
		});
	}

	/**
	 * Mixed platform + virtual thread load on the MPMC rings. Each worker fingerprints the
	 * whole buffer with its own id and verifies the pattern before releasing; any double
	 * handout (ring protocol bug or double release) corrupts the pattern and fails a
	 * verify.
	 */
	@Test
	public void testMpmcStressPlatformAndVirtualWorkers() throws InterruptedException {
		final int platformWorkers = 8;
		final int virtualWorkers = 8;
		final int workers = platformWorkers + virtualWorkers;
		final int cycles = 300;

		int baseline = ByteBufferPool.poolCount();
		var started = new CountDownLatch(workers);
		var done = new CountDownLatch(workers);
		var failure = new AtomicReference<String>();
		var allocs = new int[workers];

		List<Thread> platforms = new ArrayList<>();
		for (int worker = 0; worker < workers; worker++) {
			final int owner = worker;
			boolean virtual = owner >= platformWorkers;
			var builder = virtual ? Thread.ofVirtual() : Thread.ofPlatform();
			Thread t = builder.unstarted(() -> {
				try {
					started.countDown();
					started.await(); // all workers racing from the same instant
					for (int c = 0; c < cycles; c++) {
						byte[] buffer = ByteBufferPool.acquire();
						allocs[owner]++;
						for (int i = 0; i < buffer.length; i++) {
							buffer[i] = (byte) (i + owner * 101);
						}
						for (int i = 0; i < buffer.length; i++) {
							byte expect = (byte) (i + owner * 101);
							if (buffer[i] != expect) {
								failure.compareAndSet(null, "worker " + owner + " cycle " + c + ": [" + i + "]=" + buffer[i] + " expected " + expect);
								return;
							}
						}
						ByteBufferPool.release(buffer);
					}
				} catch (Throwable t2) {
					failure.compareAndSet(null, "worker " + owner + " threw " + t2);
				} finally {
					done.countDown();
				}
			});
			if (!virtual) {
				platforms.add(t);
			}
			t.start();
		}
		try {
			assertTrue(done.await(120, TimeUnit.SECONDS), "stress workers did not finish");
		} finally {
			for (Thread t : platforms) {
				t.join(10_000);
			}
		}
		if (failure.get() != null) {
			throw new AssertionError(failure.get());
		}
		// every acquired buffer was released exactly once, so the rings can only hold what
		// was there at the start plus the buffers each worker had to allocate when dry
		int totalAllocated = 0;
		for (int n : allocs) {
			totalAllocated += n;
		}
		int after = ByteBufferPool.poolCount();
		assertFalse(after > baseline + totalAllocated,
				"poolCount " + after + " exceeds baseline " + baseline + " + allocations " + totalAllocated);
	}
}
