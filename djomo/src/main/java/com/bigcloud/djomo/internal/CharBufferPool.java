/*******************************************************************************
 * Copyright 2026 Alex Vigdor
 *
 * adaptation of Dmitry Vyukov's bounded MPMC queue
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
package com.bigcloud.djomo.internal;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class CharBufferPool {
	public static final int IO_BUFFER_SIZE = 8192;
	public static final int SCRATCH_BUFFER_SIZE = 8192;

	public static final class CharPair {
		public final char[] io;
		public final char[] scratch;

		CharPair() {
			this.io = new char[IO_BUFFER_SIZE];
			this.scratch = new char[SCRATCH_BUFFER_SIZE];
		}
	}

	private static final class Cell {
		CharPair pair;
	}

	private static final ThreadLocal<Cell> PLATFORM_CACHE = ThreadLocal.withInitial(Cell::new);
	private static final Ring[] RINGS;
	private static final int RING_MASK;

	static {
		int stripes = roundUpPow2(Math.max(2, Runtime.getRuntime().availableProcessors() * 2));
		int maxPairs = Math.clamp(roundUpPow2(Runtime.getRuntime().availableProcessors() * 64), 512, 4096);
		int perStripe = roundUpPow2(Math.max(4, maxPairs / stripes));
		RING_MASK = stripes - 1;
		RINGS = new Ring[stripes];
		for (int i = 0; i < stripes; i++) {
			RINGS[i] = new Ring(perStripe);
		}
	}

	private CharBufferPool() {
	}

	public static CharPair acquire() {
		Thread current = Thread.currentThread();
		if (!current.isVirtual()) {
			Cell cell = PLATFORM_CACHE.get();
			CharPair local = cell.pair;
			if (local != null) {
				cell.pair = null;
				return local;
			}
		}
		return acquireFromRing(current.threadId());
	}

	public static void release(CharPair pair) {
		if (pair == null)
			return;
		Thread current = Thread.currentThread();
		if (!current.isVirtual()) {
			Cell cell = PLATFORM_CACHE.get();
			if (cell.pair == null) {
				cell.pair = pair;
				return;
			}
		}
		releaseToRing(current.threadId(), pair);
	}

	/**
	 * Number of pairs currently held in the shared rings. Pairs cached in a
	 * thread's per-thread cell, or in use by a live consumer, are not counted.
	 */
	public static int poolCount() {
		int n = 0;
		for (Ring ring : RINGS) {
			n += ring.occupied();
		}
		return n;
	}

	private static CharPair acquireFromRing(long threadId) {
		int start = stripeIndex(threadId);
		CharPair pair = RINGS[start].poll();
		if (pair != null)
			return pair;

		pair = RINGS[(start + 1) & RING_MASK].poll();
		return (pair != null) ? pair : new CharPair();
	}

	private static void releaseToRing(long threadId, CharPair pair) {
		int start = stripeIndex(threadId);
		if (!RINGS[start].offer(pair)) {
			RINGS[(start + 1) & RING_MASK].offer(pair);
		}
	}

	private static int stripeIndex(long id) {
		int hash = (int) (id ^ (id >>> 32)) * 0x9E3779B9;
		return hash & RING_MASK;
	}

	private static int roundUpPow2(int v) {
		return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
	}

	private static final class Ring {
		private static final VarHandle HEAD;
		private static final VarHandle TAIL;
		private static final VarHandle SEQ = MethodHandles.arrayElementVarHandle(long[].class);
		private static final VarHandle BUF = MethodHandles.arrayElementVarHandle(CharPair[].class);
		// CPD-OFF
		static {
			try {
				MethodHandles.Lookup l = MethodHandles.lookup();
				HEAD = l.findVarHandle(Ring.class, "head", long.class);
				TAIL = l.findVarHandle(Ring.class, "tail", long.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		@SuppressWarnings("unused")
		private long p01, p02, p03, p04, p05, p06, p07;
		@SuppressWarnings("unused")
		private volatile long head;
		@SuppressWarnings("unused")
		private long p11, p12, p13, p14, p15, p16, p17;
		@SuppressWarnings("unused")
		private volatile long tail;
		@SuppressWarnings("unused")
		private long p21, p22, p23, p24, p25, p26, p27;
		// CPD-ON
		private final CharPair[] buffer;
		private final long[] sequence;
		private final int mask;

		Ring(int capacity) {
			this.mask = capacity - 1;
			this.buffer = new CharPair[capacity];
			this.sequence = new long[capacity];
			for (int i = 0; i < capacity; i++) {
				this.sequence[i] = i;
			}
		}

		boolean offer(CharPair item) {
			// CPD-OFF
			long pos;
			while (true) {
				pos = (long) TAIL.getVolatile(this);
				int idx = (int) pos & mask;
				long seq = (long) SEQ.getVolatile(sequence, idx);
				long diff = seq - pos;
				if (diff == 0) {
					if (TAIL.weakCompareAndSet(this, pos, pos + 1)) {
						BUF.setOpaque(buffer, idx, item);
						SEQ.setRelease(sequence, idx, pos + 1);
						return true;
					}
				} else if (diff < 0) {
					return false;
				}
				Thread.onSpinWait();
			}
			// CPD-ON
		}

		CharPair poll() {
			long pos;
			while (true) {
				pos = (long) HEAD.getVolatile(this);
				int idx = (int) pos & mask;
				long seq = (long) SEQ.getVolatile(sequence, idx);
				long diff = seq - (pos + 1);
				if (diff == 0) {
					if (HEAD.weakCompareAndSet(this, pos, pos + 1)) {
						CharPair item = (CharPair) BUF.getOpaque(buffer, idx);
						BUF.setOpaque(buffer, idx, null);
						SEQ.setRelease(sequence, idx, pos + mask + 1);
						return item;
					}
				} else if (diff < 0) {
					return null;
				}
				Thread.onSpinWait();
			}
		}

		int occupied() {
			long d = (long) TAIL.getVolatile(this) - (long) HEAD.getVolatile(this);
			return d < 0 ? 0 : (int) d;
		}
	}
}
