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

import java.util.concurrent.locks.StampedLock;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;

/**
 * This implementation of {@link LockStrategy} uses {@link StampedLock} and directly dispatches all methods to analogs in {@link StampedLock}.
 */
@ThreadSafe
public final class StampedLockStrategy implements LockStrategy {
  private final StampedLock stampedLock;

  public StampedLockStrategy() {
    stampedLock = new StampedLock();
  }

  @Override
  public final long trySharedLock() {
    return stampedLock.tryReadLock();
  }

  @Override
  public final long sharedLock() {
    final long result = stampedLock.readLock();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || result != 0;
    return result;
  }

  @Override
  public final void unlockShared(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    stampedLock.unlockRead(stamp);
  }

  @Override
  public final boolean isSharedLocked() {
    return stampedLock.isReadLocked();
  }

  @Override
  public final long tryLock() {
    return stampedLock.tryWriteLock();
  }

  @Override
  public final long lock() {
    final long result = stampedLock.writeLock();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || result != 0;
    return result;
  }

  @Override
  public final void unlock(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    stampedLock.unlockWrite(stamp);
  }
}