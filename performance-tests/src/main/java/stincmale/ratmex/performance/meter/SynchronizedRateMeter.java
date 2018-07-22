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
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;

@ThreadSafe
public final class SynchronizedRateMeter<S> implements RateMeter<S> {
  private final RateMeter<S> rm;
  private final Object mutex;

  public SynchronizedRateMeter(final RateMeter<S> rm) {
    Preconditions.checkNotNull(rm, "rm");
    this.rm = rm;
    this.mutex = new Object();
  }

  @Override
  public final long getStartNanos() {
    synchronized (mutex) {
      return rm.getStartNanos();
    }
  }

  @Override
  public final Duration getSamplesInterval() {
    synchronized (mutex) {
      return rm.getSamplesInterval();
    }
  }

  @Override
  public final Duration getTimeSensitivity() {
    synchronized (mutex) {
      return rm.getTimeSensitivity();
    }
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    synchronized (mutex) {
      return rm.rightSamplesWindowBoundary();
    }
  }

  @Override
  public final long ticksCountTotal() {
    synchronized (mutex) {
      return rm.ticksCountTotal();
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    synchronized (mutex) {
      rm.tick(count, tNanos);
    }
  }

  @Override
  public final double rateAverage() {
    synchronized (mutex) {
      return rm.rateAverage();
    }
  }

  @Override
  public final double rateAverage(final Duration unit) {
    synchronized (mutex) {
      return rm.rateAverage(unit);
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    synchronized (mutex) {
      return rm.rateAverage(tNanos);
    }
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    synchronized (mutex) {
      return rm.rateAverage(tNanos, unit);
    }
  }

  @Override
  public final long rate() {
    synchronized (mutex) {
      return rm.rate();
    }
  }

  @Override
  public final RateMeterReading rate(final RateMeterReading reading) {
    synchronized (mutex) {
      return rm.rate(reading);
    }
  }

  @Override
  public final double rate(final Duration unit) {
    synchronized (mutex) {
      return rm.rate(unit);
    }
  }

  @Override
  public final RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    synchronized (mutex) {
      return rm.rate(unit, reading);
    }
  }

  @Override
  public final double rate(final long tNanos) {
    synchronized (mutex) {
      return rm.rate(tNanos);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
    synchronized (mutex) {
      return rm.rate(tNanos, reading);
    }
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    synchronized (mutex) {
      return rm.rate(tNanos, unit);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    synchronized (mutex) {
      return rm.rate(tNanos, unit, reading);
    }
  }

  @Override
  public final Optional<? extends S> stats() {
    synchronized (mutex) {
      return rm.stats();
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{rm=" + rm +
        '}';
  }
}