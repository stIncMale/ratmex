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

package stincmale.ratmex.executor;

import java.time.Duration;
import java.util.Optional;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.Immutable;
import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.internal.util.ConversionsAndChecks;
import stincmale.ratmex.internal.util.Preconditions;

/**
 * A configuration of a {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
 * <p>
 * The default values:
 * <ul>
 * <li>{@link #getInitialDelay()} - {@link Duration#ZERO}</li>
 * <li>{@link #getDuration()} - {@link Optional}{@code .}{@linkplain Optional#empty() empty()}</li>
 * <li>{@link #getRateListener()} - {@link Optional}{@code .}{@linkplain Optional#empty() empty()}</li>
 * </ul>
 *
 * @param <E> A type of container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 */
//see comments in SubmitterWorkerScheduledTaskConfig for reasons why rateListener do not have a default value different from null
@Immutable
public class ScheduledTaskConfig<E extends RateMeasuredEvent> {
  private final Duration initialDelay;
  @Nullable
  private final Duration duration;
  @Nullable
  private final RateListener<? super E> rateListener;

  /**
   * @param initialDelay See {@link Builder#setInitialDelay(Duration)}.
   * @param duration See {@link Builder#setDuration(Duration)}.
   * @param rateListener See {@link Builder#setRateListener(RateListener)}.
   */
  protected ScheduledTaskConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final RateListener<? super E> rateListener) {
    ConversionsAndChecks.checkDuration(initialDelay, "initialDelay");
    if (duration != null) {
      Preconditions.checkArgument(!duration.isZero(), "duration", "Must not be zero");
      Preconditions.checkArgument(!duration.isNegative(), "duration", "Must not be negative");
    }
    this.initialDelay = initialDelay;
    this.duration = duration;
    this.rateListener = rateListener;
  }

  public static <E extends RateMeasuredEvent> Builder<E> newScheduleConfigBuilder() {
    return new Builder<>();
  }

  public Builder<E> toBuilder() {
    return new Builder<E>().set(this);
  }

  /**
   * Specifies the time to delay the first execution of a scheduled task.
   */
  public final Duration getInitialDelay() {
    return initialDelay;
  }

  /**
   * Specifies the amount of time after the {@linkplain #getInitialDelay() initial delay} after which a scheduled task should not be executed anymore.
   * There is no guarantee beyond best-effort attempt to not exceed this duration.
   * <p>
   * An {@linkplain Optional#empty() empty} duration means that the task will be repeatedly executed
   * until one of the exceptional completions specified by
   * {@link RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig)} occur.
   */
  public final Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }

  /**
   * A listener allowing monitoring the rate and reacting if there are deviations from the
   * {@linkplain RateMeasuredEvent#getTargetRate() target rate}.
   */
  public final Optional<RateListener<? super E>> getRateListener() {
    return Optional.ofNullable(rateListener);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{initialDelay=" + initialDelay +
        ", duration=" + duration +
        ", rateListener=" + rateListener +
        '}';
  }

  @NotThreadSafe
  public static class Builder<E extends RateMeasuredEvent> {
    protected Duration initialDelay;
    @Nullable
    protected Duration duration;
    @Nullable
    protected RateListener<? super E> rateListener;

    protected Builder() {
      initialDelay = Duration.ZERO;
      duration = null;
      rateListener = null;
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E> set(final ScheduledTaskConfig<E> config) {
      Preconditions.checkNotNull(config, "config");
      initialDelay = config.getInitialDelay();
      duration = config.getDuration()
          .orElse(null);
      rateListener = config.getRateListener()
          .orElse(null);
      return this;
    }

    /**
     * @param initialDelay Must not be {@linkplain Duration#isNegative() negative}.
     *
     * @see ScheduledTaskConfig#getInitialDelay()
     */
    public final Builder<E> setInitialDelay(final Duration initialDelay) {
      ConversionsAndChecks.checkDuration(initialDelay, "initialDelay");
      this.initialDelay = initialDelay;
      return this;
    }

    /**
     * @see ScheduledTaskConfig#getDuration()
     */
    public final Builder<E> setDuration(@Nullable final Duration duration) {
      if (duration != null) {
        Preconditions.checkArgument(!duration.isZero(), "duration", "Must not be zero");
        Preconditions.checkArgument(!duration.isNegative(), "duration", "Must not be negative");
      }
      this.duration = duration;
      return this;
    }

    /**
     * @see ScheduledTaskConfig#getRateListener()
     */
    public final Builder<E> setRateListener(@Nullable final RateListener<? super E> rateListener) {
      this.rateListener = rateListener;
      return this;
    }

    public final ScheduledTaskConfig<E> buildScheduledTaskConfig() {
      return new ScheduledTaskConfig<>(
          initialDelay,
          duration,
          rateListener);
    }
  }
}