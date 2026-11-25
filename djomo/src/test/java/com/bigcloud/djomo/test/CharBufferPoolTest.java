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

import com.bigcloud.djomo.internal.CharBufferPool;
import com.bigcloud.djomo.internal.CharBufferPool.CharPair;

import static com.bigcloud.djomo.test.PoolTestSupport.onVirtual;

/**
 * Tests for the shared {@link CharBufferPool}: the per-thread cache for platform threads,
 * the ring-based branches used by virtual threads (which bypass the cache entirely), and
 * the Vyukov MPMC ring under mixed concurrent load.
 *
 * <p>
 * Exact {@link CharBufferPool#poolCount()} assertions are only deterministic on virtual
 * threads: pairs parked in a live platform thread's per-thread cell are invisible to the
 * ring accounting, while virtual threads always route acquire/release through the rings.
 * The rings are a process-wide cache shared by all tests (surefire runs the suite in a
 * single fork, sequentially), so tests measure counts relative to a reading taken on the
 * same virtual thread just before each acquire, and pin identity only when the pool is
 * empty.
 *
 * <p>
 * The stress test fingerprints every slot of every acquired pair with a worker-unique
 * pattern and verifies it before release; a ring protocol error or a double release that
 * hands the same pair to two concurrent owners corrupts a pattern and fails a verify.
 */
public class CharBufferPoolTest {

	@Test
	public void testAcquiredPairSizes() {
		CharPair pair = CharBufferPool.acquire();
		try {
			assertEquals(pair.io.length, CharBufferPool.IO_BUFFER_SIZE);
			assertEquals(pair.scratch.length, CharBufferPool.SCRATCH_BUFFER_SIZE);
		} finally {
			CharBufferPool.release(pair);
		}
	}

	@Test
	public void testReleaseNullIsNoOp() {
		int before = CharBufferPool.poolCount();
		CharBufferPool.release(null);
		assertEquals(CharBufferPool.poolCount(), before);
	}

	/**
	 * A pair released by a platform thread parks in that thread's per-thread cell, so the
	 * next acquire on the same thread gets the very same pair back (deterministic).
	 */
	@Test
	public void testPlatformCellRoundTrip() {
		CharPair first = CharBufferPool.acquire();
		CharBufferPool.release(first);
		CharPair second = CharBufferPool.acquire();
		try {
			assertSame(first, second, "a pair released by a platform thread must be returned by the next acquire on that thread");
		} finally {
			CharBufferPool.release(second);
		}
	}

	/**
	 * Reentrancy guarantee for the same thread: the per-thread cell holds at most one pair,
	 * so a re-entrant acquire while a pair is in use always gets a *different* pair. This
	 * is what lets a writer (or parser) nested inside another writer (or parser) on the
	 * same thread work without clobbering the outer owner's buffer.
	 */
	@Test
	public void testReentrantAcquireIsDistinct() {
		CharPair first = CharBufferPool.acquire();
		CharPair second = CharBufferPool.acquire();
		try {
			assertNotSame(first, second, "re-entrant acquire on the same thread must return a distinct pair");
		} finally {
			CharBufferPool.release(first);
			CharBufferPool.release(second);
		}
	}

	/**
	 * Virtual threads never touch the per-thread cell: every acquire and release goes
	 * through the rings. A ring acquire scans two stripes, so whether it hits a pooled
	 * pair depends on residue left by other tests; the exact accounting is therefore
	 * made relative to a count taken on the same virtual thread right before each
	 * acquire, and the fully deterministic case (an empty pool) pins identity.
	 */
	@Test
	public void testVirtualThreadBranchExactAccounting() {
		onVirtual(() -> {
			int before = CharBufferPool.poolCount();
			CharPair a = CharBufferPool.acquire();
			int afterA = CharBufferPool.poolCount();
			assertTrue(afterA >= before - 1 && afterA <= before,
					"a single acquire must remove at most one pair from the rings");
			CharBufferPool.release(a);
			assertEquals(CharBufferPool.poolCount(), afterA + 1, "a VT release must land in a ring");
			if (before == 0) {
				assertEquals(afterA, 0, "a fresh allocation must not change the pool count");
				CharPair b = CharBufferPool.acquire();
				assertEquals(CharBufferPool.poolCount(), 0,
						"the released pair is the pool's only pair, so this acquire must take it from a ring");
				assertSame(a, b, "a pair released to an empty pool must come straight back to the same thread");
				CharBufferPool.release(b);
				assertEquals(CharBufferPool.poolCount(), 1);
			}
			CharPair c = CharBufferPool.acquire();
			CharPair d = CharBufferPool.acquire();
			assertNotSame(c, d, "a re-entrant acquire on the same VT must return distinct pairs");
			int afterBoth = CharBufferPool.poolCount();
			CharBufferPool.release(c);
			CharBufferPool.release(d);
			assertEquals(CharBufferPool.poolCount(), afterBoth + 2, "each release must add exactly one pair");
		});
	}

	/**
	 * Mixed platform + virtual thread load on the MPMC rings. Each worker fingerprints the
	 * whole pair with its own id and verifies the pattern before releasing; any double
	 * handout (ring protocol bug or double release) corrupts a pattern and fails a verify.
	 */
	@Test
	public void testMpmcStressPlatformAndVirtualWorkers() throws InterruptedException {
		final int platformWorkers = 8;
		final int virtualWorkers = 8;
		final int workers = platformWorkers + virtualWorkers;
		final int cycles = 300;

		int baseline = CharBufferPool.poolCount();
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
						CharPair pair = CharBufferPool.acquire();
						allocs[owner]++;
						char[] io = pair.io;
						char[] scratch = pair.scratch;
						for (int i = 0; i < io.length; i++) {
							io[i] = (char) ((i + owner * 101) & 0xFFFF);
						}
						for (int i = 0; i < scratch.length; i++) {
							scratch[i] = (char) ((i * 3 + owner * 7) & 0xFFFF);
						}
						for (int i = 0; i < io.length; i++) {
							char expect = (char) ((i + owner * 101) & 0xFFFF);
							if (io[i] != expect) {
								failure.compareAndSet(null, "worker " + owner + " cycle " + c + ": io[" + i + "]=" + io[i] + " expected " + expect);
								return;
							}
						}
						for (int i = 0; i < scratch.length; i++) {
							char expect = (char) ((i * 3 + owner * 7) & 0xFFFF);
							if (scratch[i] != expect) {
								failure.compareAndSet(null, "worker " + owner + " cycle " + c + ": scratch[" + i + "]=" + scratch[i] + " expected " + expect);
								return;
							}
						}
						CharBufferPool.release(pair);
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
		// every acquired pair was released exactly once, so the rings can only hold what
		// was there at the start plus the pairs each worker had to allocate when dry
		int totalAllocated = 0;
		for (int n : allocs) {
			totalAllocated += n;
		}
		int after = CharBufferPool.poolCount();
		assertFalse(after > baseline + totalAllocated,
				"poolCount " + after + " exceeds baseline " + baseline + " + allocations " + totalAllocated);
	}
}
