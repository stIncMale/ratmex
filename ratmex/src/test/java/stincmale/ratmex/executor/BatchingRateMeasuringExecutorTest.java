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

import java.text.NumberFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import stincmale.ratmex.doc.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig.Mode;
import stincmale.ratmex.meter.auxiliary.LongTicksCounter;
import stincmale.ratmex.meter.auxiliary.ParkWaitStrategy;
import stincmale.ratmex.meter.auxiliary.StampedLockStrategy;
import stincmale.ratmex.meter.ConcurrentRingBufferRateMeter;
import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig;
import stincmale.ratmex.meter.auxiliary.LongAdderTicksCounter;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import stincmale.ratmex.meter.RingBufferRateMeter;
import static java.lang.Math.round;
import static java.time.Duration.of;
import static stincmale.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;

@TestInstance(Lifecycle.PER_METHOD)
public final class BatchingRateMeasuringExecutorTest {
  public BatchingRateMeasuringExecutorTest() {
  }

  private static final void nop() {
  }

  private final Map<Long, Runnable> batchCache = new HashMap<>();

  private final Runnable batch(final Runnable r, final long size) {
    final Runnable result;
    if (size > 1) {
      result = batchCache.computeIfAbsent(size, key -> () -> {
        for (long i = 0; i < key; i++) {
          r.run();
        }
      });
    } else {
      result = r;
    }
    return result;
  }

  private final void batchExecute(
      final Executor ex,
      final Runnable task,
      final long tasksCount,
      final long batchesCount,
      final RateMeter<?> rateMeter,
      final long tNanos) {
    final long batchSize = tasksCount / batchesCount;
    if (batchSize > 1) {
      final Runnable baseBatch = batch(task, batchSize);
      final long remainder = tasksCount - batchesCount * batchSize;
      if (remainder == 0) {
        execute(ex, baseBatch, batchesCount);
      } else {//we need to evenly spread out the remainder
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || remainder < batchesCount;
        final Runnable extendedBatch = batch(task, batchSize + 1);
        final boolean baseFirst = ThreadLocalRandom.current()
            .nextBoolean();
        if (baseFirst) {
          execute(ex, baseBatch, batchesCount - remainder);
          execute(ex, extendedBatch, remainder);
        } else {
          execute(ex, extendedBatch, remainder);
          execute(ex, baseBatch, batchesCount - remainder);
        }
      }
    } else {//batching is not appropriate
      execute(ex, task, tasksCount);
    }
    rateMeter.tick(tasksCount, tNanos);
  }

  private final void execute(
      final Executor ex,
      final Runnable task,
      final long tasksCount) {
    for (long i = 0; i < tasksCount; i++) {
      ex.execute(task);
    }
  }

  @Test
  public final void test() throws Exception {
    final int threadsCount = 5;
    final ScheduledExecutorService submitterEx = Executors.newScheduledThreadPool(1, new BoundedThreadFactory(1));
    submitterEx.submit(() -> {});
    final ExecutorService ex = Executors.newFixedThreadPool(threadsCount, new PrintThreadFactory());
    final CountDownLatch latch = new CountDownLatch(threadsCount);
    for (int i = 0; i < threadsCount; i++) {
      ex.submit(() -> {
        try {
          latch.await();
        } catch (final InterruptedException e) {
          Thread.currentThread()
              .interrupt();
        }
      });
      latch.countDown();
    }
    println("Threads have been started", 2);
    final AtomicLong submitDurationNanos = new AtomicLong();
    final Duration timeSensitivity = of(1, ChronoUnit.MILLIS);
    final Duration samplesInterval = of(100, ChronoUnit.MILLIS);
    final long targetRatePerSecond = 20_000_000;
    final long targetSubmitsTotal = targetRatePerSecond * 10L;
    final Rate targetSubmits = Rate.withRelativeDeviation(targetRatePerSecond, 0.05, Duration.ofSeconds(1));
    println("targetSubmits=" + targetSubmits + ", targetSubmits.avg=" + ((targetSubmits.getMinValue() + targetSubmits.getMaxValue()) / 2), 1);
    final long startNanos = System.nanoTime();
    final RateMeter<?> submitterRateMeter = new RingBufferRateMeter(
        startNanos,
        samplesInterval,
        RingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setTicksCounterSupplier(LongTicksCounter::new)
            .setHistoryLength(2)
            .setTimeSensitivity(timeSensitivity)
            .build());
    final ConcurrentRateMeterConfig.Builder concurrentRmCfgBuilder = ConcurrentRingBufferRateMeter.defaultConfig()
        .toBuilder()
        .setMode(Mode.RELAXED_TICKS)
        .setLockStrategySupplier(StampedLockStrategy::new)
        .setWaitStrategySupplier(ParkWaitStrategy::defaultInstance);
    concurrentRmCfgBuilder.setCollectStats(true)
        .setMaxTicksCountAttempts(5)
        .setTicksCounterSupplier(LongAdderTicksCounter::new)
        .setHistoryLength(3)
        .setTimeSensitivity(timeSensitivity);
    final RateMeter<ConcurrentRateMeterStats> completionRateMeter =
        new ConcurrentRingBufferRateMeter(startNanos, samplesInterval, concurrentRmCfgBuilder.build());
    final LongAdder globalCompleteCounter = new LongAdder();
    final Supplier<Runnable> submitterSupplier = () -> new Runnable() {
      private long submitsCounter;
      private final Runnable task;
      private final RateMeterReading submitterReading;
      private final RateMeterReading completionReading;
      private long prevTNanos;
      private final long samplesIntervalNanos;
      private final double targetSubmitsMean;

      {
        submitsCounter = 0;
        task = new RateMeterAwareRunnable(() -> {
          globalCompleteCounter.increment();
          //          ThreadLocalRandom.current()
          //              .nextGaussian();
        }, completionRateMeter);
        submitterReading = new RateMeterReading();
        completionReading = new RateMeterReading();
        samplesIntervalNanos = submitterRateMeter.getSamplesInterval()
            .toNanos();
        prevTNanos = startNanos;
        targetSubmitsMean = (targetSubmits.getMinValue() + targetSubmits.getMaxValue()) / 2;
      }

      @Override
      public final void run() {
        final long tNanos = System.nanoTime();
        final long submitsRequired = submitsRequired(tNanos);
        submitsCounter += submitsRequired;
        batchExecute(ex, task, submitsRequired, threadsCount, submitterRateMeter, tNanos);
        if (submitsRequired > 0) {
          prevTNanos = tNanos;
        }
        submitterRateMeter.rate(tNanos, submitterReading);
        completionRateMeter.rate(tNanos, completionReading);
        if (submitsCounter >= targetSubmitsTotal) {
          submitDurationNanos.set(tNanos - startNanos);
          println(Duration.ofNanos(submitDurationNanos.get()), 2);
          throw new CancellationException();
        }
      }

      private final long submitsRequired(final long tNanos) {
        final double measuredAverageSubmits = submitterRateMeter.rateAverage();
        final double deltaAverage = measuredAverageSubmits - targetSubmitsMean;
        submitterRateMeter.rate(tNanos, submitterReading);
        final long measuredSubmits = submitterReading.getValueLong();
        final double ratio = (tNanos - prevTNanos) / (double)samplesIntervalNanos;
        final long result;
        if (measuredSubmits < targetSubmitsMean) {//as expected; the interval (prevTNanos; tNanos] is fresh and we are deciding submits for it
          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
            result = round(ratio * targetSubmits.getMaxValue());
            println("1 " + result +
                ", passedT(ms)=" + Duration.ofNanos(tNanos - startNanos)
                .toMillis() +
                ", deltaT(ms)=" + Duration.ofNanos(tNanos - prevTNanos)
                .toMillis() +
                ", measuredSubmits=" + measuredSubmits +
                ", ratio=" + ratio, 1);
          } else {//too fast in average; submit as few as possible to satisfy the min
            result = round(ratio * targetSubmits.getMinValue());
            println("2 " + result, 1);
          }
        } else {//too fast
          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
            result = round(ratio * targetSubmits.getMaxValue());
            println("3 " + result, 1);
          } else {//too fast in average; do not submit anything
            result = 0;
            println("4 " + result +
                ", passedT(ms)=" + Duration.ofNanos(tNanos - startNanos)
                .toMillis() +
                ", deltaT(ms)=" + Duration.ofNanos(tNanos - prevTNanos)
                .toMillis() +
                ", measuredSubmits=" + measuredSubmits +
                ", ratio=" + ratio, 1);
          }
        }
        //        if (measuredSubmits < targetSubmitsMean) {//as expected; the interval (prevTNanos; tNanos] is fresh and we are deciding submits for it
        //          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
        //            result = measuredSubmits < targetSubmits.getMaxValue()
        //                ? round(ratio * (targetSubmits.getMaxValue() - measuredSubmits))
        //                : 0;
        //            println("1 " + result +
        //                ", passedT(ms)=" + Duration.ofNanos(tNanos - startNanos)
        //                .toMillis() +
        //                ", deltaT(ms)=" + Duration.ofNanos(tNanos - prevTNanos)
        //                .toMillis() +
        //                ", measuredSubmits=" + measuredSubmits +
        //                ", ratio=" + ratio);
        //          } else {//too fast in average; submit as few as possible to satisfy the min
        //            result = measuredSubmits < targetSubmits.getMinValue()
        //                ? round(ratio * (targetSubmits.getMinValue() - measuredSubmits))
        //                : 0;
        //            println("2 " + result);
        //          }
        //        } else {//too fast
        //          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
        //            result = measuredSubmits < targetSubmits.getMaxValue()
        //                ? round(ratio * (targetSubmits.getMaxValue() - measuredSubmits))
        //                : 0;
        //            println("3 " + result);
        //          } else {//too fast in average; do not submit anything
        //            result = 0;
        //            println("4 " + result);
        //          }
        //        }
        return result;
      }
    };
    final Future<?> submitting = submitterEx.scheduleAtFixedRate(
        submitterSupplier.get(),
        0,
        submitterRateMeter.getTimeSensitivity()
            .toNanos(),
        TimeUnit.NANOSECONDS);
    try {
      submitting.get();
    } catch (final ExecutionException e) {
      if (!(e.getCause() instanceof CancellationException)) {
        throw e;
      }
    }
    {
      Locale.setDefault(Locale.ROOT);
      final NumberFormat format = NumberFormat.getIntegerInstance();
      format.setGroupingUsed(true);
      submitterRateMeter.rateAverage(Duration.ofSeconds(1));
      println("submitterRateMeter.ticksCountTotal=" + format.format(submitterRateMeter.ticksCountTotal()) +
          ", submitterRateMeter.rateAverage=" + format.format(submitterRateMeter.rateAverage(Duration.ofSeconds(1))) +
          ", submitterRateMeter.rate=" + format.format(submitterRateMeter.rate(Duration.ofSeconds(1))), 2);
      println("completionRateMeter.ticksCountTotal=" + format.format(completionRateMeter.ticksCountTotal()) +
          ", completionRateMeter.rateAverage=" + format.format((completionRateMeter.rateAverage(Duration.ofSeconds(1)))) +
          ", completionRateMeter.rate=" + format.format(completionRateMeter.rate(Duration.ofSeconds(1))), 2);
      println("globalCompleteCounter=" + format.format(globalCompleteCounter) +
          ", globalCompleteCounter/submitDurationNanos=" + format.format((double)globalCompleteCounter.sum() /
          ((double)submitDurationNanos.get() / (double)Duration.ofSeconds(1)
              .toNanos())), 2);
      println("targetRatePerSecond=" + format.format(targetRatePerSecond) +
          ", targetSubmits=" + targetSubmits, 2);
      //      println("failedAccuracyEventsCountForTick=" + submitterRateMeter.stats()
      //          .map(ConcurrentRateMeterStats::failedAccuracyEventsCountForTick)
      //          .orElse(0L) +
      //          ", failedAccuracyEventsCountForRate=" + submitterRateMeter.stats()
      //          .map(ConcurrentRateMeterStats::failedAccuracyEventsCountForRate)
      //          .orElse(0L), 2);
      println("batchCache.size=" + batchCache.size(), 2);
    }
  }

  private static final class BoundedThreadFactory implements ThreadFactory {
    private final int max;
    private final AtomicInteger counter;

    private BoundedThreadFactory(final int max) {
      this.max = max;
      counter = new AtomicInteger(0);
    }

    @Nullable
    @Override
    public final Thread newThread(final Runnable r) {
      @Nullable
      final Thread result;
      final int currentCount = counter.get();
      if (currentCount < max && counter.compareAndSet(currentCount, currentCount + 1)) {//omit CAS when possible (similar to DCL idiom)
        result = new Thread(r);
      } else {
        result = null;
      }
      println(result, 2);
      return result;
    }
  }

  private static final class PrintThreadFactory implements ThreadFactory {
    private PrintThreadFactory() {
    }

    @Override
    public final Thread newThread(final Runnable r) {
      final Thread result = new Thread(r);
      println(result, 2);
      return result;
    }
  }

  private static final class RateMeterAwareRunnable implements Runnable {
    private final Runnable r;
    private final RateMeter<?> rateMeter;

    private RateMeterAwareRunnable(final Runnable r, final RateMeter<?> rateMeter) {
      this.r = r;
      this.rateMeter = rateMeter;
    }

    @Override
    public final void run() {
      r.run();
      rateMeter.tick(1, System.nanoTime());
    }
  }

  private static final void println(final Object o, final int level) {
    if (level > 1) {
      System.out.println(o);
    }
  }
}