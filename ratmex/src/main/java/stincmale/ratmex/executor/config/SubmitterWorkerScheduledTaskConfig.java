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
import java.util.function.BiFunction;
import java.util.function.Supplier;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.Immutable;
import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.executor.AbstractSubmitterWorkerRateMeasuringExecutorService;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.RateMeasuringExecutorService;
import stincmale.ratmex.executor.listener.RateListener;
import stincmale.ratmex.executor.listener.RateMeasuredEvent;
import stincmale.ratmex.meter.AbstractRateMeter;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.config.RateMeterConfig;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration of a
 * {@linkplain AbstractSubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig) scheduled task}
 * for {@link AbstractSubmitterWorkerRateMeasuringExecutorService}.
 * <p>
 * The default values:
 * <ul>
 * <li>The default values from {@link ScheduledTaskConfig}</li>
 * <li>{@link #getSubmitterRateMeterSupplier()} - Must be explicitly specified, there is no default value</li>
 * <li>{@link #getWorkerRateMeterSupplier()} - Must be explicitly specified, there is no default value</li>
 * </ul>
 *
 * @param <E> A type of container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@Immutable
public class SubmitterWorkerScheduledTaskConfig<E extends RateMeasuredEvent, SRS, WRS> extends ScheduledTaskConfig<E> {
  private final BiFunction<Long, Duration, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
  private final BiFunction<Long, Duration, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

  /**
   * @param initialDelay See {@link ScheduledTaskConfig#ScheduledTaskConfig(Duration, Duration, Supplier)}.
   * @param duration See {@link ScheduledTaskConfig#ScheduledTaskConfig(Duration, Duration, Supplier)}.
   * @param rateListenerSupplier See {@link ScheduledTaskConfig#ScheduledTaskConfig(Duration, Duration, Supplier)}.
   * @param submitterRateMeterSupplier See {@link Builder#Builder(BiFunction, BiFunction)}.
   * @param workerRateMeterSupplier See {@link Builder#Builder(BiFunction, BiFunction)}.
   */
  protected SubmitterWorkerScheduledTaskConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final Supplier<? extends RateListener<? super E>> rateListenerSupplier,
      final BiFunction<Long, Duration, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
      final BiFunction<Long, Duration, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
    super(initialDelay, duration, rateListenerSupplier);
    checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
    checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
    this.submitterRateMeterSupplier = submitterRateMeterSupplier;
    this.workerRateMeterSupplier = workerRateMeterSupplier;
  }

  /**
   * @param submitterRateMeterSupplier See {@link SubmitterWorkerScheduledTaskConfig#getSubmitterRateMeterSupplier()}. Must not be null.
   * @param workerRateMeterSupplier See {@link SubmitterWorkerScheduledTaskConfig#getWorkerRateMeterSupplier()}. Must not be null.
   */
  public static <E extends RateMeasuredEvent, SRS, WRS> Builder<E, SRS, WRS> newSubmitterWorkerScheduledTaskConfigBuilder(
      final BiFunction<Long, Duration, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
      final BiFunction<Long, Duration, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
    checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
    checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
    return new Builder<>(submitterRateMeterSupplier, workerRateMeterSupplier);
  }

  @Override
  public Builder<E, SRS, WRS> toBuilder() {
    return new Builder<E, SRS, WRS>(this.submitterRateMeterSupplier, this.workerRateMeterSupplier).set(this);
  }

  /**
   * Specifies a supplier which must be used by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}
   * to create a {@link RateMeter} for measuring the submission rate.
   * <p>
   * Usage example: {@code config.getSubmitterRateMeterSupplier().apply(startNanos, sampleInterval)},
   * where {@code startNanos} and {@code sampleInterval} are the first to arguments for
   * {@link AbstractRateMeter#AbstractRateMeter(long, Duration, RateMeterConfig)}.
   */
  public final BiFunction<Long, Duration, ? extends RateMeter<? extends SRS>> getSubmitterRateMeterSupplier() {
    return submitterRateMeterSupplier;
  }

  /**
   * Specifies a supplier which must be used by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}
   * to create a {@link RateMeter} for measuring the completion rate.
   * <p>
   * Usage example: {@code config.getWorkerRateMeterSupplier().apply(startNanos, sampleInterval)},
   * where {@code startNanos} and {@code sampleInterval} are the first to arguments for
   * {@link AbstractRateMeter#AbstractRateMeter(long, Duration, RateMeterConfig)}.
   */
  public final BiFunction<Long, Duration, ? extends RateMeter<? extends WRS>> getWorkerRateMeterSupplier() {
    return workerRateMeterSupplier;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{initialDelay=" + getInitialDelay() +
        ", duration=" + getDuration() +
        ", rateListenerSupplier=" + getRateListenerSupplier() +
        ", submitterRateMeterSupplier" + submitterRateMeterSupplier +
        ", workerRateMeterSupplier" + workerRateMeterSupplier +
        '}';
  }

  @NotThreadSafe
  public static class Builder<E extends RateMeasuredEvent, SRS, WRS> extends ScheduledTaskConfig.Builder<E> {
    protected BiFunction<Long, Duration, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
    protected BiFunction<Long, Duration, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

    /**
     * @param submitterRateMeterSupplier See {@link SubmitterWorkerScheduledTaskConfig#getSubmitterRateMeterSupplier()}. Must not be null.
     * @param workerRateMeterSupplier See {@link SubmitterWorkerScheduledTaskConfig#getWorkerRateMeterSupplier()}. Must not be null.
     */
    protected Builder(
        final BiFunction<Long, Duration, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
        final BiFunction<Long, Duration, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
      checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
      checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
        this.submitterRateMeterSupplier = submitterRateMeterSupplier;
        this.workerRateMeterSupplier = workerRateMeterSupplier;
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E, SRS, WRS> set(final SubmitterWorkerScheduledTaskConfig<? super E, ? extends SRS, ? extends WRS> config) {
      checkNotNull(config, "config");
      super.set(config);
      submitterRateMeterSupplier = config.getSubmitterRateMeterSupplier();
      workerRateMeterSupplier = config.getWorkerRateMeterSupplier();
      return this;
    }

    public final SubmitterWorkerScheduledTaskConfig<E, SRS, WRS> buildSubmitterWorkerScheduledTaskConfig() {
      return new SubmitterWorkerScheduledTaskConfig<>(
          initialDelay,
          duration,
          rateListenerSupplier,
          submitterRateMeterSupplier,
          workerRateMeterSupplier);
    }
  }
}