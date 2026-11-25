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

import static org.testng.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Shared helpers for the buffer pool tests.
 *
 * <p>
 * The pools are process-wide singletons and the tests share them: a test must not
 * assume the rings are empty when it starts, because a preceding test may have
 * left buffers there. Exact {@code poolCount()} accounting is only deterministic
 * on virtual threads — buffers parked in a live platform thread's per-thread cell
 * are invisible to the ring accounting, while virtual threads always route every
 * acquire/release through the rings — and even then only relative to a count
 * measured on the same virtual thread immediately before the acquire: a ring
 * acquire scans two stripes, so whether it hits a pooled buffer depends on the
 * residue other tests left in those stripes.
 */
public final class PoolTestSupport {

	private PoolTestSupport() {
	}

	/**
	 * Run {@code task} on a virtual thread and wait for completion, rethrowing its
	 * exception (an {@code AssertionError} is rethrown as-is).
	 */
	public static void onVirtual(Runnable task) {
		onVirtual(() -> {
			task.run();
			return null;
		});
	}

	/**
	 * Run {@code task} on a virtual thread and return its result (or rethrow its exception).
	 */
	public static <T> T onVirtual(Supplier<T> task) {
		var result = new AtomicReference<T>();
		var error = new AtomicReference<Throwable>();
		var finished = new CountDownLatch(1);
		Thread.ofVirtual().start(() -> {
			try {
				result.set(task.get());
			} catch (Throwable t) {
				error.set(t);
			} finally {
				finished.countDown();
			}
		});
		try {
			assertTrue(finished.await(60, TimeUnit.SECONDS), "virtual thread task did not finish in time");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
		if (error.get() != null) {
			if (error.get() instanceof AssertionError) {
				throw (AssertionError) error.get();
			}
			throw new AssertionError("virtual thread task failed", error.get());
		}
		return result.get();
	}
}
