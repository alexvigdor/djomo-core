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

public final class ByteBufferPool {
	public static final int BUFFER_SIZE = 8192;

	private static final class Cell {
		byte[] buf;
	}

	private static final ThreadLocal<Cell> PLATFORM_CACHE = ThreadLocal.withInitial(Cell::new);
	private static final Ring[] RINGS;
	private static final int RING_MASK;

	static {
		int stripes = roundUpPow2(Math.max(2, Runtime.getRuntime().availableProcessors() * 2));
		int maxBuffers = Math.clamp(roundUpPow2(Runtime.getRuntime().availableProcessors() * 64), 512, 4096);
		int perStripe = roundUpPow2(Math.max(4, maxBuffers / stripes));
		RING_MASK = stripes - 1;
		RINGS = new Ring[stripes];
		for (int i = 0; i < stripes; i++) {
			RINGS[i] = new Ring(perStripe);
		}
	}

	private ByteBufferPool() {
	}

	public static byte[] acquire() {
		Thread current = Thread.currentThread();
		if (!current.isVirtual()) {
			Cell cell = PLATFORM_CACHE.get();
			byte[] local = cell.buf;
			if (local != null) {
				cell.buf = null;
				return local;
			}
		}
		return acquireFromRing();
	}

	public static void release(byte[] buffer) {
		if (buffer == null || buffer.length != BUFFER_SIZE)
			return;
		Thread current = Thread.currentThread();
		if (!current.isVirtual()) {
			Cell cell = PLATFORM_CACHE.get();
			if (cell.buf == null) {
				cell.buf = buffer;
				return;
			}
		}
		releaseToRing(buffer);
	}

	/**
	 * Number of buffers currently held in the shared rings. Buffers cached in a
	 * thread's per-thread cell, or in use by a live consumer, are not counted.
	 */
	public static int poolCount() {
		int n = 0;
		for (Ring ring : RINGS) {
			n += ring.occupied();
		}
		return n;
	}

	private static int stripeIndex() {
		long id = Thread.currentThread().threadId();
		int hash = (int) (id ^ (id >>> 32)) * 0x9E3779B9;
		return hash & RING_MASK;
	}

	public static int roundUpPow2(int v) {
		return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
	}

	private static byte[] acquireFromRing() {
		int start = stripeIndex();
		byte[] buf = RINGS[start].poll();
		if (buf != null)
			return buf;

		buf = RINGS[(start + 1) & RING_MASK].poll();
		return (buf != null) ? buf : new byte[BUFFER_SIZE];
	}

	private static void releaseToRing(byte[] buffer) {
		int start = stripeIndex();
		if (!RINGS[start].offer(buffer)) {
			RINGS[(start + 1) & RING_MASK].offer(buffer);
		}
	}

	private static final class Ring {
		private static final VarHandle HEAD;
		private static final VarHandle TAIL;
		private static final VarHandle SEQ = MethodHandles.arrayElementVarHandle(long[].class);
		private static final VarHandle BUF = MethodHandles.arrayElementVarHandle(byte[][].class);
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
		private final byte[][] buffer;
		private final long[] sequence;
		private final int mask;

		Ring(int capacity) {
			this.mask = capacity - 1;
			this.buffer = new byte[capacity][]; // Lazy: all slots start null
			this.sequence = new long[capacity];
			for (int i = 0; i < capacity; i++) {
				this.sequence[i] = i;
			}
		}

		boolean offer(byte[] item) {
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

		byte[] poll() {
			long pos;
			while (true) {
				pos = (long) HEAD.getVolatile(this);
				int idx = (int) pos & mask;
				long seq = (long) SEQ.getVolatile(sequence, idx);
				long diff = seq - (pos + 1);
				if (diff == 0) {
					if (HEAD.weakCompareAndSet(this, pos, pos + 1)) {
						byte[] item = (byte[]) BUF.getOpaque(buffer, idx);
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
