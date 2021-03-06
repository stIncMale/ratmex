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

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.checkDuration;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * This implementation of {@link WaitStrategy} combines exponential backoff and {@link LockSupport#park() parking}.
 */
@ThreadSafe
public final class ParkWaitStrategy implements WaitStrategy {
  private static final ParkWaitStrategy instance = new ParkWaitStrategy(Duration.ofNanos(50), Duration.ofNanos(300));

  private final Duration minDelay;
  private final long minDelayNanos;
  private final Duration maxDelay;
  private final long maxDelayNanos;

  /**
   * @param minDelay The lower desired bound of the blocking time intervals between tests of a {@linkplain #await(BooleanSupplier) condition}.
   * Must not be {@link Duration#isNegative() negative}.
   * @param maxDelay The upper desired bound of the blocking time intervals between tests of a {@linkplain #await(BooleanSupplier) condition}.
   * There is no guarantee beyond best-effort attempt to not exceed this duration.
   * Must not be {@link Duration#isNegative() negative}.
   */
  public ParkWaitStrategy(final Duration minDelay, final Duration maxDelay) {
    checkDuration(minDelay, "minDelay");
    checkDuration(maxDelay, "maxDelay");
    checkArgument(maxDelay.compareTo(minDelay) > 0, "maxDelay", () -> format("Must be greater than %s=%s", "minDelay", minDelay));
    this.minDelay = minDelay;
    minDelayNanos = minDelay.toNanos();
    this.maxDelay = maxDelay;
    maxDelayNanos = maxDelay.toNanos();
  }

  /**
   * Always returns the same instance with:
   * <ul>
   * <li>{@code minDelay} 50ns;</li>
   * <li>{@code maxDelay} 300ns.</li>
   * </ul>
   *
   * @return An instance of {@link ParkWaitStrategy}.
   */
  public static final ParkWaitStrategy defaultInstance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    checkNotNull(condition, "condition");
    if (!condition.getAsBoolean()) {
      boolean interrupted = false;
      try {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long minDelayNanos = this.minDelayNanos;
        long maxDelayNanos = Math.min(minDelayNanos * 2, this.maxDelayNanos);
        boolean maxReached = false;
        do {
          Thread.onSpinWait();
          final long delayNanos = rnd.nextLong(minDelayNanos, maxDelayNanos);
          LockSupport.parkNanos(condition, rnd.nextLong(delayNanos));
          if (Thread.interrupted()) {
            interrupted = true;
          }
          if (!maxReached) {
            final long doubledMaxDelayNanos = maxDelayNanos * 2;
            if (doubledMaxDelayNanos < this.maxDelayNanos) {
              minDelayNanos = maxDelayNanos;
              maxDelayNanos = doubledMaxDelayNanos;
            } else {
              maxReached = true;
              maxDelayNanos = this.maxDelayNanos;
            }
          }
        } while (!condition.getAsBoolean());
      } finally {
        if (interrupted) {
          Thread.currentThread()
              .interrupt();
        }
      }
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{minDelay=" + minDelay +
        ", maxDelay=" + maxDelay +
        '}';
  }
}