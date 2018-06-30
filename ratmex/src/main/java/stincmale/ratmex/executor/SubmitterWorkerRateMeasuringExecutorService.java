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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.executor.config.SubmitterWorkerScheduledTaskConfig;
import stincmale.ratmex.executor.listener.DefaultSubmitterWorkerRateListener;
import stincmale.ratmex.executor.listener.RateListener;
import stincmale.ratmex.executor.listener.SubmitterWorkerRateMeasuredEvent;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import stincmale.ratmex.meter.ConcurrentRingBufferRateMeter;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;
import stincmale.ratmex.meter.RingBufferRateMeter;
import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig;
import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig.Mode;
import static stincmale.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * An implementation of {@link AbstractSubmitterWorkerRateMeasuringExecutorService} which works with
 * worker {@link RateMeter}s exposing {@link ConcurrentRateMeterStats}.
 * This implementation produces {@link SubmitterWorkerRateMeasuredEvent}s for {@link RateListener}s.
 */
@ThreadSafe
public final class SubmitterWorkerRateMeasuringExecutorService extends AbstractSubmitterWorkerRateMeasuringExecutorService<
    SubmitterWorkerScheduledTaskConfig<SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>, Object, ConcurrentRateMeterStats>,
    SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>,
    Object, ConcurrentRateMeterStats> {
  private static final SubmitterWorkerScheduledTaskConfig<
      SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>, Object, ConcurrentRateMeterStats> defaultScheduledTaskConfig;

  static {
    final SubmitterWorkerScheduledTaskConfig.Builder<
        SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>, Object, ConcurrentRateMeterStats>
        builder = SubmitterWorkerScheduledTaskConfig.newSubmitterWorkerScheduledTaskConfigBuilder(
        RingBufferRateMeter::new,
        (startNanos, samplesInterval) -> new ConcurrentRingBufferRateMeter(
            startNanos,
            samplesInterval,
            ConcurrentRingBufferRateMeter.defaultConfig()
              .toBuilder()
              .setMode(Mode.RELAXED_TICKS)
              .setCollectStats(true)
              .build()));
    builder.setRateListenerSupplier(DefaultSubmitterWorkerRateListener::instance);
    defaultScheduledTaskConfig = builder.buildSubmitterWorkerScheduledTaskConfig();
  }

  /**
   * Returns a default {@link SubmitterWorkerScheduledTaskConfig} with
   * <ul>
   *  <li>
   *    {@linkplain RingBufferRateMeter#RingBufferRateMeter(long, Duration) Default} {@link RingBufferRateMeter}
   *    as {@linkplain SubmitterWorkerScheduledTaskConfig#getSubmitterRateMeterSupplier() submitter rate meter}
   *  </li>
   *  <li>
   *    {@link ConcurrentRingBufferRateMeter} with modified {@linkplain ConcurrentRingBufferRateMeter#defaultConfig() default}
   *    {@link ConcurrentRateMeterConfig}
   *    <ul>
   *      <li>{@link ConcurrentRateMeterConfig#getMode()} - {@link Mode#RELAXED_TICKS}
   *      </li>
   *      <li>{@link ConcurrentRateMeterConfig#isCollectStats()} - true</li>
   *    </ul>
   *    as {@linkplain SubmitterWorkerScheduledTaskConfig#getWorkerRateMeterSupplier() worker rate meter}.
   *  </li>
   * </ul>
   * <p>
   * Note that despite using {@link Mode#RELAXED_TICKS}, {@link DefaultSubmitterWorkerRateListener}
   * {@linkplain DefaultSubmitterWorkerRateListener#onMeasurement(SubmitterWorkerRateMeasuredEvent) detects}
   * any incorrectly registered ticks by using {@link ConcurrentRateMeterStats} thus guaranteeing correctness of all measurements.
   */
  public static final SubmitterWorkerScheduledTaskConfig<
    SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>, Object, ConcurrentRateMeterStats> defaultScheduledTaskConfig() {
    return defaultScheduledTaskConfig;
  }

  /**
   * See {@link AbstractSubmitterWorkerRateMeasuringExecutorService#AbstractSubmitterWorkerRateMeasuringExecutorService(ScheduledExecutorService, ExecutorService, int, boolean)}.
   */
  public SubmitterWorkerRateMeasuringExecutorService(
    final ScheduledExecutorService submitter,
    final ExecutorService worker,
    final int workerThreadsCount,
    final boolean shutdownSubmitterAndWorker) {
    super(submitter, worker, workerThreadsCount, shutdownSubmitterAndWorker);
  }

  /**
   * See {@link AbstractSubmitterWorkerRateMeasuringExecutorService#AbstractSubmitterWorkerRateMeasuringExecutorService(ThreadFactory, ThreadFactory, int, boolean)}.
   */
  public SubmitterWorkerRateMeasuringExecutorService(
    final ThreadFactory submitterThreadFactory,
    final ThreadFactory workerThreadFactory,
    final int threadsCount,
    final boolean prestartThreads) {
    super(submitterThreadFactory, workerThreadFactory, threadsCount, prestartThreads);
  }

  /**
   * See {@link AbstractSubmitterWorkerRateMeasuringExecutorService#AbstractSubmitterWorkerRateMeasuringExecutorService(int, boolean)}.
   */
  public SubmitterWorkerRateMeasuringExecutorService(final int threadsCount, final boolean prestartThreads) {
    super(threadsCount, prestartThreads);
  }

  /**
   * @param targetRate {@inheritDoc}
   * {@link Rate#getUnit()} is used as {@linkplain RateMeter#getSamplesInterval() samples interval}, thus two mathematically identical rates
   * <pre>{@code
   * Rate rateMs = Rate.withRelativeDeviation(10, 0.1, Duration.ofMillis(1));
   * Rate rateS = Rate.withRelativeDeviation(10_000, 0.1, Duration.ofSeconds(1))
   * }</pre>
   * impose different constraints on the uniformity of the rate distribution over time: {@code rateMs} imposes a more even distribution by requiring
   * 10 ± 1 tasks to be executed roughly every 1ms, while {@code rateS} requires 10_000 ± 1000 tasks to be executed roughly every 1s,
   * thus allowing arbitrary (but still limited, e.g. 0, or 42) number of tasks to be executed at some millisecond intervals.
   * Hence, maintaining {@code rateMs} may be more difficult than maintaining {@code rateS}.
   * This is something to consider in situations when task execution duration is expected to be varying.
   */
  @Override
  protected final ScheduledFuture<?> scheduleAtFixedRate(
      final Runnable task,
      final Rate targetRate,
      final SubmitterWorkerScheduledTaskConfig<
          SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>, Object, ConcurrentRateMeterStats> config,
      final ScheduledExecutorService submitter,
      final ExecutorService worker) {
    checkNotNull(task, "task");
    checkNotNull(targetRate, "targetRate");
    checkNotNull(config, "config");
    checkNotNull(submitter, "submitter");
    checkNotNull(submitter, "worker");
    final long currentNanos = System.nanoTime();
    final long delayNanos = config.getInitialDelay()
        .toNanos();
    final long startNanos = currentNanos + delayNanos;//this may overflow, and it is as intended
    final Duration sampleInterval = targetRate.getUnit();
    final RateMeter<?> submitterRateMeter = config.getSubmitterRateMeterSupplier()
        .apply(startNanos, sampleInterval);
    final RateMeter<? extends ConcurrentRateMeterStats> workerRateMeter = config.getWorkerRateMeterSupplier()
        .apply(startNanos, sampleInterval);
    final long periodNanos = submitterRateMeter.getTimeSensitivity()
        .toNanos();
    @Nullable
    final RateListener<? super SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>> rateListener = config.getRateListenerSupplier()
        .map(Supplier::get)
        .orElseGet(null);
    @Nullable
    final SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats> rateMeasuredEvent = rateListener == null
        ? null
        : new SubmitterWorkerRateMeasuredEvent<>(
            targetRate, new RateMeterReading(), new RateMeterReading(), null, workerRateMeter.stats()
            .orElse(null));
    final SubmitterTask<SubmitterWorkerRateMeasuredEvent<Object, ConcurrentRateMeterStats>, Object, ConcurrentRateMeterStats>
        submitterTask = new SubmitterTask<>(targetRate, submitterRateMeter, workerRateMeter, rateListener, rateMeasuredEvent);
    final ScheduledFuture<?> result = submitter.scheduleAtFixedRate(submitterTask, delayNanos, periodNanos, TimeUnit.NANOSECONDS);
    submitterTask.setExternallyVisibleFuture(result);
    return result;
  }

  private static final class SubmitterTask<E extends SubmitterWorkerRateMeasuredEvent<? extends SRS, ? extends WRS>, SRS, WRS> implements Runnable {
    private final RateMeter<? extends SRS> submitterRateMeter;
    private final RateMeter<? extends WRS> workerRateMeter;
    @Nullable
    private final RateListener<? super E> rateListener;
    @Nullable
    private final E rateMeasuredEvent;
    @Nullable
    private volatile ScheduledFuture<?> externallyVisibleFuture;

    private SubmitterTask(
      final Rate targetRate,
      final RateMeter<? extends SRS> submitterRateMeter,
      final RateMeter<? extends WRS> workerRateMeter,
      @Nullable final RateListener<? super E> rateListener,
      @Nullable final E rateMeasuredEvent) {
      this.submitterRateMeter = submitterRateMeter;
      this.workerRateMeter = workerRateMeter;
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE ||
        (rateListener == null && rateMeasuredEvent == null) || (rateListener != null && rateMeasuredEvent != null);
      this.rateListener = rateListener;
      this.rateMeasuredEvent = rateMeasuredEvent;
    }

    private final void setExternallyVisibleFuture(final ScheduledFuture<?> future) {
      externallyVisibleFuture = future;
    }

    @Override
    public final void run() {
      final long tNanos = System.nanoTime();
      //TODO implement
      submitterRateMeter.rate(tNanos, rateMeasuredEvent.getSubmissionRate());
      if (rateListener != null) {
        workerRateMeter.rate(tNanos, rateMeasuredEvent.getCompletionRate());
        rateListener.onMeasurement(rateMeasuredEvent);
      }
    }
  }
}