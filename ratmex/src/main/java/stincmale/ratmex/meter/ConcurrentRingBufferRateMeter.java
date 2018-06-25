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

import java.time.Duration;
import java.util.Optional;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.meter.auxiliary.AtomikLongArray;
import stincmale.ratmex.meter.auxiliary.LongArray;
import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig;

/**
 * A thread-safe implementation of {@link AbstractRingBufferRateMeter},
 * which uses {@link AtomikLongArray} implementation of {@link LongArray} (this might be changed in the future).
 */
@ThreadSafe
public final class ConcurrentRingBufferRateMeter extends AbstractRingBufferRateMeter<ConcurrentRateMeterStats, ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig = ConcurrentRateMeterConfig.newBuilder()
      .build();

  @Nullable
  private final ConcurrentRingBufferRateMeterStats stats;

  /**
   * @return A default configuration, which is the default {@link ConcurrentRateMeterConfig}.
   */
  public static final ConcurrentRateMeterConfig defaultConfig() {
    return defaultConfig;
  }

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   */
  public ConcurrentRingBufferRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRateMeterConfig config) {
    super(startNanos, samplesInterval, config, AtomikLongArray::new, false);
    stats = config.isCollectStats() ? new ConcurrentRingBufferRateMeterStats() : null;
  }

  /**
   * This constructor is equivalent to {@link #ConcurrentRingBufferRateMeter(long, Duration, ConcurrentRateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public ConcurrentRingBufferRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig);
  }

  /**
   * @return An {@linkplain Optional#empty() empty} {@link Optional}.
   */
  @Override
  public final Optional<ConcurrentRateMeterStats> stats() {
    return Optional.ofNullable(stats);
  }

  @Override
  protected final void registerIncorrectlyRegisteredTicksEvent() {
    if (stats != null) {
      stats.registerIncorrectlyRegisteredTicksEvent();
    }
  }
}