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

package stincmale.ratmex.performance.meter;

import java.time.Duration;
import java.util.Optional;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.internal.util.Preconditions;
import stincmale.ratmex.meter.LockStrategy;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;

@ThreadSafe
public final class ConcurrentSimpleRateMeter<S> implements RateMeter<S> {
  private final RateMeter<S> rm;
  private final LockStrategy lockStrategy;

  public ConcurrentSimpleRateMeter(final RateMeter<S> rm, final LockStrategy lockStrategy) {
    Preconditions.checkNotNull(rm, "rm");
    Preconditions.checkNotNull(lockStrategy, "lockStrategy");
    this.rm = rm;
    this.lockStrategy = lockStrategy;
  }

  @Override
  public final long getStartNanos() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.getStartNanos();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final Duration getSamplesInterval() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.getSamplesInterval();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final Duration getTimeSensitivity() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.getTimeSensitivity();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rightSamplesWindowBoundary();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long ticksCountTotal() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.ticksCountTotal();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    final long lockStamp = lockStrategy.lock();
    try {
      rm.tick(count, tNanos);
    } finally {
      lockStrategy.unlock(lockStamp);
    }
  }

  @Override
  public final double rateAverage() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage(unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage(tNanos);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage(tNanos, unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long rate() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(unit, reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final long tNanos) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos, reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos, unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos, unit, reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final Optional<S> stats() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.stats();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{rm=" + rm +
        ", lockStrategy=" + lockStrategy +
        '}';
  }
}