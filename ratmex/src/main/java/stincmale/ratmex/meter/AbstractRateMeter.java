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
import stincmale.ratmex.common.Configurable;
import stincmale.ratmex.internal.util.Preconditions;
import stincmale.ratmex.meter.auxiliary.TicksCounter;
import stincmale.ratmex.meter.config.RateMeterConfig;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.checkTNanos;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.checkUnit;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.maxTNanos;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.convertRate;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * A generic implementation of a {@linkplain Configurable configurable} {@link RateMeter}.
 *
 * @param <S> A type that represents {@linkplain #stats() statistics}.
 * @param <C> A type of the {@linkplain #getConfig() configuration}.
 */
public abstract class AbstractRateMeter<S, C extends RateMeterConfig> implements RateMeter<S>, Configurable<C> {
  /**
   * @see RateMeterConfig#getTimeSensitivity()
   */
  private static final int DEFAULT_SAMPLES_INTERVAL_OVER_TIME_SENSITIVITY_RATIO = 20;
  private final TicksCounter ticksTotal;
  private final long startNanos;
  private final Duration samplesInterval;
  private final long samplesIntervalNanos;
  private final Duration timeSensitivity;
  private final long timeSensitivityNanos;
  private final long maxTNanos;
  private final C config;

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   */
  protected AbstractRateMeter(final long startNanos, final Duration samplesInterval, final C config) {
    checkNotNull(samplesInterval, "samplesInterval");
    Preconditions.checkArgument(!samplesInterval.isZero(), "samplesInterval", "Must not be zero");
    Preconditions.checkArgument(!samplesInterval.isNegative(), "samplesInterval", "Must be positive");
    checkNotNull(config, "config");
    this.startNanos = startNanos;
    this.samplesInterval = samplesInterval;
    samplesIntervalNanos = samplesInterval.toNanos();
    Preconditions.checkArgument(samplesIntervalNanos <= Long.MAX_VALUE / (config.getHistoryLength() + 1) - 1, "samplesInterval",
        () -> format(
            "Must be less than (Long.MAX_VALUE - 1)nanos = %snanos, but actual value is %s",
            Long.MAX_VALUE - 1,
            samplesIntervalNanos));
    timeSensitivity = config.getTimeSensitivity()
        .orElseGet(() -> {
          Preconditions.checkArgument(samplesIntervalNanos % DEFAULT_SAMPLES_INTERVAL_OVER_TIME_SENSITIVITY_RATIO == 0, "config",
              () -> format(
                  "samplesIntervalNanos must be a multiple of %s because timeSensitivity is not specified",
                  DEFAULT_SAMPLES_INTERVAL_OVER_TIME_SENSITIVITY_RATIO));
          return Duration.ofNanos(samplesIntervalNanos / DEFAULT_SAMPLES_INTERVAL_OVER_TIME_SENSITIVITY_RATIO);
        });
    timeSensitivityNanos = timeSensitivity.toNanos();
    maxTNanos = maxTNanos(startNanos, samplesIntervalNanos, config.getHistoryLength() + 1);
    this.config = config;
    ticksTotal = config.getTicksCounterSupplier()
        .apply(0L);
  }

  @Override
  public final long getStartNanos() {
    return startNanos;
  }

  @Override
  public final Duration getSamplesInterval() {
    return samplesInterval;
  }

  @Override
  public final Duration getTimeSensitivity() {
    return timeSensitivity;
  }

  @Override
  public final long ticksCountTotal() {
    return ticksTotal.get();
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rateAverage(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rate(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public final C getConfig() {
    return config;
  }

  /**
   * @return A counter that must be used to register and calculate total ticks count.
   * {@link #ticksCountTotal()} returns {@link #getTicksTotalCounter()}{@code .}{@link TicksCounter#get() get()}.
   */
  protected final TicksCounter getTicksTotalCounter() {
    return ticksTotal;
  }

  /**
   * Should be used instead of {@link #getSamplesInterval()}{@code .}{@link Duration#toNanos() toNanos()} to avoid unnecessary calculations.
   *
   * @return {@link #getSamplesInterval()} in nanoseconds.
   */
  protected final long getSamplesIntervalNanos() {
    return samplesIntervalNanos;
  }

  /**
   * Should be used instead of {@link #getTimeSensitivity()}{@code .}{@link Duration#toNanos() toNanos()} to avoid unnecessary calculations.
   *
   * @return {@link #getTimeSensitivity()} in nanoseconds.
   */
  protected final long getTimeSensitivityNanos() {
    return timeSensitivityNanos;
  }

  /**
   * This method should be used to check {@code tNanos} argument in methods such as {@link #rate(long)}, {@link #tick(long, long)} etc.
   *
   * @param tNanos A checked argument.
   * @param safeParamName A parameter name that will be used as is without any checks.
   *
   * @throws IllegalArgumentException If the argument is invalid.
   */
  protected final void checkArgument(final long tNanos, final String safeParamName) throws IllegalArgumentException {
    checkTNanos(tNanos, startNanos, maxTNanos, safeParamName);
  }

  /**
   * This method should be used to check {@code unit} argument in methods such as {@link #rate(Duration)}, etc.
   *
   * @param unit A checked argument.
   * @param safeParamName A parameter name that will be used as is without any checks.
   *
   * @throws IllegalArgumentException If the argument is invalid.
   */
  protected final void checkArgument(final Duration unit, final String safeParamName) throws IllegalArgumentException {
    checkUnit(unit, safeParamName);
  }

  /**
   * This method should be used to check both {@code tNanos} and {@code unit} arguments in methods such as {@link #rate(long, Duration)}, etc.
   * The method sequentially invokes methods
   * {@link #checkArgument(long, String)}, {@link #checkArgument(Duration, String)}.
   *
   * @param tNanos A checked argument.
   * @param safeTNanosParamName A {@code tNanos} parameter name that will be used as is without any checks.
   * @param unit A checked argument.
   * @param safeUnitParamName A {@code unit} parameter name that will be used as is without any checks.
   *
   * @throws IllegalArgumentException If either argument is invalid.
   */
  protected final void checkArguments(
      final long tNanos, final String safeTNanosParamName,
      final Duration unit, final String safeUnitParamName) throws IllegalArgumentException {
    checkTNanos(tNanos, startNanos, maxTNanos, safeTNanosParamName);
    checkUnit(unit, safeUnitParamName);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{startNanos=" + startNanos +
        ", samplesIntervalNanos=" + samplesIntervalNanos +
        ", config=" + config +
        '}';
  }
}