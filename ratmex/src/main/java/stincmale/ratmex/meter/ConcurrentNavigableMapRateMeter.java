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
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.common.NanosComparator;

/**
 * A thread-safe implementation of {@link AbstractNavigableMapRateMeter},
 * which uses {@link ConcurrentSkipListMap} implementation of {@link NavigableMap} (this might be changed in the future).
 */
@ThreadSafe
public final class ConcurrentNavigableMapRateMeter extends AbstractNavigableMapRateMeter<ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig = (ConcurrentRateMeterConfig)ConcurrentRateMeterConfig.newBuilder()
      .setHistoryLength(4)
      .build();

  /**
   * @return A default configuration. The default values:
   * <ul>
   * <li>The default values from {@link ConcurrentRateMeterConfig} except for {@link ConcurrentRateMeterConfig#getHistoryLength()}</li>
   * <li>{@link ConcurrentRateMeterConfig#getHistoryLength()} - 4</li>
   * </ul>.
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
  public ConcurrentNavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new ConcurrentSkipListMap<>(NanosComparator.instance()), false);
  }

  /**
   * This constructor is equivalent to {@link #ConcurrentNavigableMapRateMeter(long, Duration, ConcurrentRateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public ConcurrentNavigableMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig);
  }
}