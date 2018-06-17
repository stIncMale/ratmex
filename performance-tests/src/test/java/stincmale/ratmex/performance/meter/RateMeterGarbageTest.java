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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.meter.ConcurrentRingBufferRateMeter;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;

/**
 * Use Java Mission Control or a similar tool to monitor JVM heap.
 */
@Disabled
@TestInstance(Lifecycle.PER_CLASS)
public class RateMeterGarbageTest {
  private static final int NUMBER_OF_THREADS = 32;
  private static final Duration runDuration = Duration.ofMinutes(5);
  private static ThreadLocal<RateMeterReading> reading = ThreadLocal.withInitial(RateMeterReading::new);

  private ExecutorService ex;

  public RateMeterGarbageTest() {
  }

  @BeforeEach
  public final void beforeEach() {
    ex = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
  }

  @AfterEach
  public final void afterEach() throws InterruptedException {
    ex.shutdownNow();
    ex.awaitTermination(2, TimeUnit.SECONDS);
  }

  @Test
  public void run() throws RunnerException, InterruptedException {
    final RateMeter<?> rm = new ConcurrentRingBufferRateMeter(System.nanoTime(), Duration.ofMillis(1),
        ConcurrentRingBufferRateMeter.defaultConfig()
            .toBuilder()
            //            .setWaitStrategySupplier(YieldWaitStrategy::instance)
            //            .setLockStrategySupplier(() -> new SpinLockStrategy(YieldWaitStrategy.instance()))
            .build());
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      ex.submit(() -> useRateMeterForRunDuration(rm));
    }
    Thread.sleep(runDuration.toMillis());
  }

  private static final void useRateMeterForRunDuration(final RateMeter<?> rm) {
    while (System.nanoTime() - rm.getStartNanos() < runDuration.toNanos() &&
        !Thread.currentThread()
            .isInterrupted()) {
      useRateMeter(rm);
    }
  }

  private static final void useRateMeter(final RateMeter<?> rm) {
    for (int i = 0; i < 20_000_000; i++) {
      final long tNanos = System.nanoTime();
      if ((i & 3) == 0) {
        rm.rateAverage(tNanos);
        rm.rate(reading.get());
        rm.rate(tNanos, reading.get());
      } else {
        rm.tick(1, tNanos);
      }
    }
  }
}