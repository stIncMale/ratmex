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

package stincmale.ratmex.meter.auxiliary;

import stincmale.ratmex.doc.ThreadSafe;

/**
 * A lock abstraction which allows implementations with different semantics,
 * e.g. a simple lock or a two-phase (read/write, shared/exclusive) lock.
 */
@ThreadSafe
public interface LockStrategy {
  /**
   * Non-exclusively acquires the lock if it is immediately available.
   * Implementations are free to implement this method more strictly by exclusively acquiring the lock.
   *
   * @return A stamp that can be used to {@linkplain #unlockShared(long) unlock}, or 0 if the lock is not available.
   */
  long trySharedLock();

  /**
   * Non-exclusively acquires the lock, blocking if necessary until available.
   * Implementations are free to implement this method more strictly by exclusively acquiring the lock.
   *
   * @return A stamp (not 0) that can be used to {@linkplain #unlockShared(long) unlock}.
   */
  long sharedLock();

  /**
   * If the lock state matches the given stamp, releases the non-exclusive lock.
   *
   * @param stamp A stamp returned by a non-exclusive-lock operation. Must not be 0.
   *
   * @throws IllegalMonitorStateException If the stamp is not 0 and does not match the current state of this lock.
   */
  void unlockShared(long stamp);

  /**
   * @return true if the lock is currently being held non-exclusively (or exclusively, if the implementation uses only exclusive locking),
   * false otherwise.
   */
  boolean isSharedLocked();

  /**
   * Exclusively acquires the lock if it is immediately available.
   *
   * @return A stamp that can be used to {@linkplain #unlock(long) unlock}, or 0 if the lock is not available.
   */
  long tryLock();

  /**
   * Exclusively acquires the lock, blocking if necessary until available.
   *
   * @return A stamp (not 0) that can be used to {@linkplain #unlock(long) unlock}.
   */
  long lock();

  /**
   * If the lock state matches the given stamp, releases the non-exclusive lock.
   *
   * @param stamp A stamp returned by an exclusive-lock operation. Must not be 0.
   *
   * @throws IllegalMonitorStateException If the stamp is not 0 and does not match the current state of this lock.
   */
  void unlock(long stamp);
}