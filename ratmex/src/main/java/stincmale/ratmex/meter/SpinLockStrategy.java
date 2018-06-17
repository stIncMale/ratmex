/*
 * Copyright 2017-2018 Valiantsin Kavalenka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stincmale.ratmex.meter;

import java.util.concurrent.atomic.AtomicBoolean;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.internal.util.Preconditions;

/**
 * This implementation of {@link LockStrategy} does not distinguish shared and exclusive locking.
 * An {@link AtomicBoolean} is used as a state of this lock, and blocking is implemented via a {@link WaitStrategy}.
 */
@ThreadSafe
public final class SpinLockStrategy implements LockStrategy {
  private final AtomicBoolean atomicBoolean;
  private final WaitStrategy waitStrategy;

  /**
   * @param waitStrategy A {@link WaitStrategy} used for blocking. Must not be null.
   */
  public SpinLockStrategy(final WaitStrategy waitStrategy) {
    Preconditions.checkNotNull(waitStrategy, "waitStrategy");
    atomicBoolean = new AtomicBoolean();
    this.waitStrategy = waitStrategy;
  }

  /**
   * See {@link #tryLock()}.
   */
  @Override
  public final long trySharedLock() {
    return tryLock();
  }

  /**
   * See {@link #lock()}.
   */
  @Override
  public final long sharedLock() {
    return lock();
  }

  /**
   * See {@link #unlock(long)}.
   */
  @Override
  public final void unlockShared(final long stamp) {
    Preconditions.checkArgument(stamp != 0, "stamp", "Must not be 0");
    unlock(stamp);
  }

  /**
   * @return true if the lock is currently being held exclusively, false otherwise.
   */
  @Override
  public final boolean isSharedLocked() {
    return atomicBoolean.get();
  }

  /**
   * @return {@inheritDoc} If lock have been acquired, returns 1.
   */
  @Override
  public final long tryLock() {
    return atomicBoolean.compareAndSet(false, true) ? 1 : 0;
  }

  /**
   * @return {@inheritDoc} Returns 1.
   */
  @Override
  public final long lock() {
    waitStrategy.await(() -> atomicBoolean.compareAndSet(false, true));
    return 1;
  }

  @Override
  public final void unlock(final long stamp) {
    Preconditions.checkArgument(stamp != 0, "stamp", "Must not be 0");
    atomicBoolean.set(false);
  }
}