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

package stincmale.ratmex.executor.config;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.Immutable;
import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.RateMeasuringExecutorService;
import stincmale.ratmex.executor.listener.RateListener;
import stincmale.ratmex.executor.listener.RateMeasuredEvent;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.checkDuration;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration of a {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
 * <p>
 * The default values:
 * <ul>
 * <li>{@link #getInitialDelay()} - {@link Duration#ZERO}</li>
 * <li>{@link #getDuration()} - {@link Optional}{@code .}{@linkplain Optional#empty() empty()}</li>
 * <li>{@link #getRateListenerSupplier()} - {@link Optional}{@code .}{@linkplain Optional#empty() empty()}</li>
 * </ul>
 *
 * @param <E> A type of container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 */
@Immutable
public class ScheduledTaskConfig<E extends RateMeasuredEvent> {
  private final Duration initialDelay;
  @Nullable
  private final Duration duration;
  @Nullable
  private final Supplier<? extends RateListener<? super E>> rateListenerSupplier;

  /**
   * @param initialDelay See {@link Builder#setInitialDelay(Duration)}.
   * @param duration See {@link Builder#setDuration(Duration)}.
   * @param rateListenerSupplier See {@link Builder#setRateListenerSupplier(Supplier)}.
   */
  protected ScheduledTaskConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final Supplier<? extends RateListener<? super E>> rateListenerSupplier) {
    checkDuration(initialDelay, "initialDelay");
    if (duration != null) {
      checkArgument(!duration.isZero(), "duration", "Must not be zero");
      checkArgument(!duration.isNegative(), "duration", "Must not be negative");
    }
    this.initialDelay = initialDelay;
    this.duration = duration;
    this.rateListenerSupplier = rateListenerSupplier;
  }

  public static <E extends RateMeasuredEvent> Builder<E> newScheduledTaskConfigBuilder() {
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
   * A supplier of a listener allowing monitoring the rate and reacting if there are deviations from the
   * {@linkplain RateMeasuredEvent#getTargetRate() target rate}.
   */
  public final Optional<Supplier<? extends RateListener<? super E>>> getRateListenerSupplier() {
    return Optional.ofNullable(rateListenerSupplier);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{initialDelay=" + initialDelay +
        ", duration=" + duration +
        ", rateListenerSupplier=" + rateListenerSupplier +
        '}';
  }

  @NotThreadSafe
  public static class Builder<E extends RateMeasuredEvent> {
    protected Duration initialDelay;
    @Nullable
    protected Duration duration;
    @Nullable
    protected Supplier<? extends RateListener<? super E>> rateListenerSupplier;

    protected Builder() {
      initialDelay = Duration.ZERO;
      duration = null;
      rateListenerSupplier = null;
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E> set(final ScheduledTaskConfig<? super E> config) {
      checkNotNull(config, "config");
      initialDelay = config.getInitialDelay();
      duration = config.getDuration()
          .orElse(null);
      rateListenerSupplier = config.getRateListenerSupplier()
          .orElse(null);
      return this;
    }

    /**
     * @param initialDelay Must not be {@linkplain Duration#isNegative() negative}.
     *
     * @see ScheduledTaskConfig#getInitialDelay()
     */
    public final Builder<E> setInitialDelay(final Duration initialDelay) {
      checkDuration(initialDelay, "initialDelay");
      this.initialDelay = initialDelay;
      return this;
    }

    /**
     * @see ScheduledTaskConfig#getDuration()
     */
    public final Builder<E> setDuration(@Nullable final Duration duration) {
      if (duration != null) {
        checkArgument(!duration.isZero(), "duration", "Must not be zero");
        checkArgument(!duration.isNegative(), "duration", "Must not be negative");
      }
      this.duration = duration;
      return this;
    }

    /**
     * @see ScheduledTaskConfig#getRateListenerSupplier()
     */
    public final Builder<E> setRateListenerSupplier(@Nullable final Supplier<? extends RateListener<? super E>> rateListenerSupplier) {
      this.rateListenerSupplier = rateListenerSupplier;
      return this;
    }

    public final ScheduledTaskConfig<E> buildScheduledTaskConfig() {
      return new ScheduledTaskConfig<>(
          initialDelay,
          duration,
          rateListenerSupplier);
    }
  }
}