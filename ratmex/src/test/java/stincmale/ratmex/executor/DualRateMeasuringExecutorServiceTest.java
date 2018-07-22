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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static stincmale.ratmex.executor.DualRateMeasuringExecutorService.defaultScheduledTaskConfig;

public final class DualRateMeasuringExecutorServiceTest {
  public DualRateMeasuringExecutorServiceTest() {
  }

  @Test
  public final void constructor() {
    final CountingThreadFactory stf = new CountingThreadFactory();
    final ScheduledThreadPoolExecutor submitter = new ScheduledThreadPoolExecutor(1, stf);
    submitter.setMaximumPoolSize(1);
    final CountingThreadFactory wtf = new CountingThreadFactory();
    final int workerThreadsCount = 4;
    final ExecutorService worker = new ThreadPoolExecutor(
      workerThreadsCount,
      workerThreadsCount,
      0L,
      TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<>(),
      wtf);
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(
      submitter, worker, workerThreadsCount, true)) {
      assertEquals(0, stf.count());
      assertEquals(0, wtf.count());
      ex.execute(() -> {});
      assertEquals(0, stf.count());
      assertEquals(1, wtf.count());
      ex.scheduleAtFixedRate(() -> {}, Rate.withRelativeDeviation(10, 0.1, ofMillis(1)), defaultScheduledTaskConfig());
      assertEquals(1, stf.count());
    }
  }

  @Test
  public final void constructorPrestart1() {
    final CountingThreadFactory stf = new CountingThreadFactory();
    final CountingThreadFactory wtf = new CountingThreadFactory();
    final int threadsCount = 5;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(stf, wtf, threadsCount, true)) {
      assertNotNull(ex);//refer to ex to hide a warning
      assertEquals(1, stf.count());
      assertEquals(threadsCount - 1, wtf.count());
    }
  }

  @Test
  public final void constructorPrestart2() {
    final CountingThreadFactory stf = new CountingThreadFactory();
    final CountingThreadFactory wtf = new CountingThreadFactory();
    final int threadsCount = 1;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(stf, wtf, threadsCount, true)) {
      assertNotNull(ex);//refer to ex to hide a warning
      assertEquals(1, stf.count());
      assertEquals(threadsCount - 1, wtf.count());
    }
  }

  @Test
  public final void constructorNoPrestart() {
    final CountingThreadFactory stf = new CountingThreadFactory();
    final CountingThreadFactory wtf = new CountingThreadFactory();
    final int threadsCount = 5;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(stf, wtf, threadsCount, false)) {
      assertEquals(0, stf.count());
      assertEquals(0, wtf.count());
      ex.execute(() -> {});
      assertEquals(1, stf.count());
      assertEquals(threadsCount - 1, wtf.count());
    }
  }

  @Test
  public final void shutdownWithSubmitterAndWorker1() throws InterruptedException {
    final ScheduledExecutorService submitter = Executors.newScheduledThreadPool(1);
    final ExecutorService worker = Executors.newFixedThreadPool(2);
    final DualRateMeasuringExecutorService executor;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(
        submitter, worker, -1, true)) {
      executor = ex;
      assertFalse(submitter.isShutdown());
      assertFalse(worker.isShutdown());
      assertFalse(submitter.isTerminated());
      assertFalse(worker.isTerminated());
      assertFalse(ex.isShutdown());
      assertFalse(ex.isTerminated());
      assertFalse(ex.awaitTermination(10, TimeUnit.MILLISECONDS));
    }
    assertTrue(submitter.isShutdown());
    assertTrue(worker.isShutdown());
    assertTrue(submitter.isTerminated());
    assertTrue(worker.isTerminated());
    assertTrue(executor.isShutdown());
    assertTrue(executor.isTerminated());
    assertTrue(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
  }

  @Test
  public final void shutdownWithSubmitterAndWorker2() throws InterruptedException {
    final ScheduledExecutorService submitter = Executors.newScheduledThreadPool(1);
    submitter.shutdownNow();
    final ExecutorService worker = Executors.newFixedThreadPool(2);
    worker.shutdownNow();
    final DualRateMeasuringExecutorService executor;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(
        submitter, worker, -1, true)) {
      executor = ex;
      assertTrue(submitter.isShutdown());
      assertTrue(worker.isShutdown());
      assertTrue(submitter.isTerminated());
      assertTrue(worker.isTerminated());
      assertFalse(ex.isShutdown());
      assertFalse(ex.isTerminated());
      assertFalse(ex.awaitTermination(10, TimeUnit.MILLISECONDS));
    }
    assertTrue(executor.isShutdown());
    assertTrue(executor.isTerminated());
    assertTrue(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
  }

  @Test
  public final void shutdownWithoutSubmitterAndWorker1() throws InterruptedException {
    final ScheduledExecutorService submitter = Executors.newScheduledThreadPool(1);
    final ExecutorService worker = Executors.newFixedThreadPool(2);
    final DualRateMeasuringExecutorService executor;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(
        submitter, worker, -1, false)) {
      executor = ex;
      assertFalse(submitter.isShutdown());
      assertFalse(worker.isShutdown());
      assertFalse(submitter.isTerminated());
      assertFalse(worker.isTerminated());
      assertFalse(ex.isShutdown());
      assertFalse(ex.isTerminated());
      assertFalse(ex.awaitTermination(10, TimeUnit.MILLISECONDS));
    }
    assertFalse(submitter.isShutdown());
    assertFalse(worker.isShutdown());
    assertFalse(submitter.isTerminated());
    assertFalse(worker.isTerminated());
    assertTrue(executor.isShutdown());
    assertTrue(executor.isTerminated());
    assertTrue(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
  }

  @Test
  public final void shutdownWithoutSubmitterAndWorker2() throws InterruptedException {
    final ScheduledExecutorService submitter = Executors.newScheduledThreadPool(1);
    submitter.shutdownNow();
    final ExecutorService worker = Executors.newFixedThreadPool(2);
    worker.shutdownNow();
    final DualRateMeasuringExecutorService executor;
    try (final DualRateMeasuringExecutorService ex = new DualRateMeasuringExecutorService(
        submitter, worker, -1, false)) {
      executor = ex;
      assertTrue(submitter.isShutdown());
      assertTrue(worker.isShutdown());
      assertTrue(submitter.isTerminated());
      assertTrue(worker.isTerminated());
      assertFalse(ex.isShutdown());
      assertFalse(ex.isTerminated());
      assertFalse(ex.awaitTermination(10, TimeUnit.MILLISECONDS));
    }
    assertTrue(executor.isShutdown());
    assertTrue(executor.isTerminated());
    assertTrue(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
  }

  private static final class CountingThreadFactory implements ThreadFactory {
    private AtomicInteger count;

    private CountingThreadFactory() {
      count = new AtomicInteger(0);
    }

    @Override
    public final Thread newThread(final Runnable r) {
      count.getAndIncrement();
      return new Thread(r);
    }

    private final int count() {
      return count.get();
    }
  }
}