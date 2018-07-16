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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.executor.auxiliary.Time;
import stincmale.ratmex.executor.config.ScheduledTaskConfig;
import stincmale.ratmex.executor.config.SubmitterWorkerScheduledTaskConfig;
import stincmale.ratmex.executor.listener.RateListener;
import stincmale.ratmex.executor.listener.RateMeasuredEvent;
import stincmale.ratmex.executor.listener.SubmitterWorkerRateMeasuredEvent;
import stincmale.ratmex.meter.RateMeter;
import static java.lang.Math.max;
import static stincmale.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This {@link RateMeasuringExecutorService} assigns two roles to its threads: submitter and worker.
 * Threads belonging to different roles are organized in two {@link ExecutorService}'s named submitter and worker.
 * A submitter submits tasks to a worker with the target rate. A worker executes the submitted tasks as fast as it can.
 * If the worker manages to continuously execute all the tasks being submitted without accumulating them,
 * then the tasks are not only being submitted but also are being completed with the target rate.
 * <p>
 * <b>Implementation notes</b><br>
 * This class uses {@link RateMeter} to measure rate.
 * <p>
 * A submitter always has exactly 1 thread, and a worker may have an arbitrary fixed number of threads
 * including 0 and varying number of threads.
 * If a worker has no threads, then a submitter takes over the worker role and executes tasks by itself.
 * <p>
 * Submitting tasks by a submitter to a worker means that the submitter and the worker threads communicate.
 * Inter-thread communication is always bad for performance, so it should be reduced to a minimum.
 * In order to do so a submitter organizes tasks in batches before submitting them to a worker.
 * Batching allows submitting multiple tasks to a worker via a single {@linkplain ExecutorService#execute(Runnable) act of communication}
 * between a submitter and a worker. However such a technique should be used with caution,
 * because it may induce an uneven load on worker threads and hence lead to a violation of the target rate of completion of tasks.
 * In order to use batching a submitter must know the number of worker threads (which must be fixed)
 * and distribute batched tasks evenly among worker threads.
 *
 * @param <C> A type of scheduled task config used in {@link #scheduleAtFixedRate(Runnable, Rate, C)}.
 * @param <E> A type of container with data provided to a {@linkplain ScheduledTaskConfig#getRateListenerSupplier() rate listener}.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@ThreadSafe
public abstract class AbstractSubmitterWorkerRateMeasuringExecutorService<
    C extends SubmitterWorkerScheduledTaskConfig<E, ? extends SRS, ? extends WRS>,
    E extends SubmitterWorkerRateMeasuredEvent<? extends SRS, ? extends WRS>, SRS, WRS>
    implements RateMeasuringExecutorService<C, E> {
  private static final BlockingQueue<Runnable> emptyQueue = new LinkedBlockingQueue<>(1);

  private final Time time;
  /**
   * This field is only used to simplify implementation of methods like {@link #shutdown()}, {@link #awaitTermination(long, TimeUnit)}, etc.
   */
  private final ExecutorService self;
  private final ScheduledExecutorService submitter;
  @Nullable
  private final ExecutorService worker;
  private final int workerThreadsCount;
  private final AtomicBoolean startAllThreads;
  private final boolean shutdownSubmitterAndWorker;

  /**
   * This constructor wraps provided submitter and worker thus giving a user the maximum agility and control.
   *
   * @param submitter An externally provided submitter. Must have exactly 1 thread (the creation of the thread may be delayed);
   * if this constraint is violated, then the behavior is unspecified.
   * @param worker An externally provided worker. May have an arbitrary number of threads,
   * including 0 (current-thread executor) and varying number of threads.
   * The submitter submits tasks to the worker by using {@link ExecutorService#execute(Runnable)}.
   * @param workerThreadsCount The {@linkplain ThreadPoolExecutor#getMaximumPoolSize() maximum}
   * (and the {@linkplain ThreadPoolExecutor#getCorePoolSize() core}) number of threads in the worker executor.
   * {@code workerThreadsCount} \u2208 [-1,{@link Integer#MAX_VALUE}].
   * Use -1 if the number of worker threads is unknown or is varying, but in this case batching of tasks will be disabled.
   * If {@code workerThreadsCount} >= 0, then the number of worker threads must never be changed once they all were started.
   * If this constraint is violated, then the {@link RateMeasuringExecutorService} may fail to conform to the target rate because of batching.
   * @param shutdownSubmitterAndWorker A flag that specifies whether the externally provided submitter and the worker must be
   * shut down when this {@link ExecutorService} is shutting down.
   * @param time {@link Time} that must be used by this {@link AbstractSubmitterWorkerRateMeasuringExecutorService}.
   */
  protected AbstractSubmitterWorkerRateMeasuringExecutorService(
      final ScheduledExecutorService submitter,
      final ExecutorService worker,
      final int workerThreadsCount,
      final boolean shutdownSubmitterAndWorker,
      final Time time) {
    this(
        checkNotNull(submitter, "submitter"),
        checkNotNull(worker, "worker"),
        workerThreadsCount,
        false,
        shutdownSubmitterAndWorker,
        checkNotNull(time, "time"));
  }

  /**
   * This constructor creates submitter and worker threads by using {@code submitterThreadFactory} and {@code workerThreadFactory} respectively.
   *
   * @param submitterThreadFactory A thread factory for submitter threads.
   * @param workerThreadFactory A thread factory for worker threads.
   * @param threadsCount A total number of both submitter and worker threads. {@code threadsCount} \u2208 [1,{@link Integer#MAX_VALUE}].
   * If {@code threadsCount} is 1, then the number of worker threads is 0, and the submitter takes over worker role and executes tasks by itself;
   * one may provide the same thread factory as both {@code submitterThreadFactory}, {@code workerThreadFactory} in this case.
   * @param prestartThreads A flag that specifies if all submitter and worker threads must be started
   * upon the construction of {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, causing them to idly wait for work.
   * If false, then all threads are started as soon as the first task is submitted to this executor via any of the exposed methods
   * (e.g. {@linkplain #scheduleAtFixedRate(Runnable, Rate, C)}, {@link #execute(Runnable)}, etc.).
   * @param time {@link Time} that must be used by this {@link AbstractSubmitterWorkerRateMeasuringExecutorService}.
   *
   * @see #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig, ScheduledExecutorService, ExecutorService)
   */
  protected AbstractSubmitterWorkerRateMeasuringExecutorService(
      final ThreadFactory submitterThreadFactory,
      final ThreadFactory workerThreadFactory,
      final int threadsCount,
      final boolean prestartThreads,
      final Time time) {
    this(
        createSubmitter(checkNotNull(submitterThreadFactory, "submitterThreadFactory"), prestartThreads),
        createWorker(checkNotNull(workerThreadFactory, "workerThreadFactory"), checkThreadsCountPositive(threadsCount) - 1, prestartThreads),
        threadsCount - 1,
        !prestartThreads,
        true,
        checkNotNull(time, "time"));
  }

  /**
   * This constructor creates submitter and worker threads by using {@link Executors#defaultThreadFactory()}
   * and prepends either {@code "submitter-"} or {@code "worker-"} to each {@linkplain Thread#getName() thread name} respectively.
   *
   * @param threadsCount A total number of both submitter and worker threads. {@code threadsCount} \u2208 [1,{@link Integer#MAX_VALUE}].
   * If {@code threadsCount} is 1, then the number of worker threads is 0, and the submitter takes over worker role and executes tasks by itself.
   * @param prestartThreads A flag that specifies if all submitter and worker threads must be started
   * upon the construction of {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, causing them to idly wait for work.
   * If false, then all threads are started as soon as the first task is submitted to this executor via any of the exposed methods
   * (e.g. {@linkplain #scheduleAtFixedRate(Runnable, Rate, C)}, {@link #execute(Runnable)}, etc.).
   * @param time {@link Time} that must be used by this {@link AbstractSubmitterWorkerRateMeasuringExecutorService}.
   */
  protected AbstractSubmitterWorkerRateMeasuringExecutorService(final int threadsCount, final boolean prestartThreads, final Time time) {
    this(
        createSubmitter(null, prestartThreads),
        createWorker(null, checkThreadsCountPositive(threadsCount) - 1, prestartThreads),
        threadsCount - 1,
        !prestartThreads,
        true,
        checkNotNull(time, "time"));
  }

  private AbstractSubmitterWorkerRateMeasuringExecutorService(
      final ScheduledExecutorService submitter,
      @Nullable final ExecutorService worker,
      final int workerThreadsCount,
      final boolean startAllThreadsOnFirstTask,
      final boolean shutdownSubmitterAndWorker,
      final Time time) {
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || submitter != null;
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || (worker == null && workerThreadsCount == 0) || worker != null;
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || workerThreadsCount >= -1;
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || time != null;
    this.time = time;
    this.self = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS, emptyQueue, runnable -> null);
    this.submitter = submitter;
    this.worker = worker;
    this.workerThreadsCount = workerThreadsCount;
    this.startAllThreads = new AtomicBoolean(startAllThreadsOnFirstTask);
    this.shutdownSubmitterAndWorker = shutdownSubmitterAndWorker;
  }

  /**
   * @return {@link Time} that must be used by this {@link AbstractSubmitterWorkerRateMeasuringExecutorService}.
   */
  protected final Time getTime() {
    return time;
  }

  /**
   * @return The worker. In situations when submitter takes over the worker role,
   * this method returns the submitter.
   */
  private final ExecutorService getWorker() {
    return worker == null ? submitter : worker;
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
   * @param config {@inheritDoc}
   * {@linkplain C#getRateListenerSupplier() Provides} {@link RateListener}, which method {@link RateListener#onMeasurement(RateMeasuredEvent)}
   * is called each time the submitter measures the current submission rate,
   * which in turn happens each time the submitter decides if new tasks need to be submitted to the worker.
   */
  @Override
  public final ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate targetRate, final C config) throws RejectedExecutionException {
    checkNotNull(task, "task");
    checkNotNull(targetRate, "targetRate");
    checkNotNull(config, "config");
    ensureActiveAndAllThreadsStarted();
    return scheduleAtFixedRate(task, targetRate, config, submitter, getWorker());
  }

  /**
   * This method is called by {@link #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig)}
   * and effectively implements all the logic.
   *
   * @param task See {@link #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig)}.
   * @param targetRate See {@link #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig)}.
   * @param config See {@link #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig)}.
   * @param submitter The submitter.
   * @param worker The worker. In cases when the submitter takes over the worker role, this is the same as {@code submitter}.
   *
   * @return See {@link #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig)}.
   *
   * @throws RejectedExecutionException See {@link #scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig)}.
   */
  protected abstract ScheduledFuture<?> scheduleAtFixedRate(
      final Runnable task,
      final Rate targetRate,
      final C config,
      final ScheduledExecutorService submitter,
      final ExecutorService worker) throws RejectedExecutionException;

  @Override
  public final void close() {
    RateMeasuringExecutorService.super.close();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Also forwards calls to the submitter and to the worker
   * if and only if those were created by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, or were
   * {@linkplain #AbstractSubmitterWorkerRateMeasuringExecutorService(ScheduledExecutorService, ExecutorService, int, boolean, Time) provided externally}
   * with {@code shutdownSubmitterAndWorker} option enabled.
   */
  @Override
  public final void shutdown() {
    try {
      self.shutdown();
    } finally {
      if (shutdownSubmitterAndWorker) {
        try {
          submitter.shutdown();
        } finally {
          if (worker != null) {
            worker.shutdown();
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Also forwards calls to the submitter and to the worker
   * if and only if those were created by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, or were
   * {@linkplain #AbstractSubmitterWorkerRateMeasuringExecutorService(ScheduledExecutorService, ExecutorService, int, boolean, Time) provided externally}
   * with {@code shutdownSubmitterAndWorker} option enabled.
   *
   * @return A list that contains combined results from the submitter and the worker,
   * or an {@linkplain Collections#emptyList() empty list} if the call was not forwarded to them.
   */
  @Override
  public final List<Runnable> shutdownNow() {
    final List<Runnable> result;
    try {
      self.shutdownNow();
    } finally {
      if (shutdownSubmitterAndWorker) {
        result = new ArrayList<>();
        try {
          result.addAll(submitter.shutdownNow());
        } finally {
          if (worker != null) {
            result.addAll(worker.shutdownNow());
          }
        }
      } else {
        result = Collections.emptyList();
      }
    }
    return result;
  }

  @Override
  public final boolean isShutdown() {
    return self.isShutdown();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Also forwards calls to the submitter and to the worker
   * if and only if those were created by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, or were
   * {@linkplain #AbstractSubmitterWorkerRateMeasuringExecutorService(ScheduledExecutorService, ExecutorService, int, boolean, Time) provided externally}
   * with {@code shutdownSubmitterAndWorker} option enabled.
   */
  @Override
  public final boolean isTerminated() {
    return self.isTerminated() &&
      (!shutdownSubmitterAndWorker || (submitter.isTerminated() && (worker == null || worker.isTerminated())));
  }

  /**
   * {@inheritDoc}
   * <p>
   * Also forwards calls to the submitter and to the worker
   * if and only if those were created by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, or were
   * {@linkplain #AbstractSubmitterWorkerRateMeasuringExecutorService(ScheduledExecutorService, ExecutorService, int, boolean, Time) provided externally}
   * with {@code shutdownSubmitterAndWorker} option enabled.
   *
   * @param timeout {@inheritDoc}.
   * There is no guarantee beyond best-effort attempt to not exceed this duration.
   */
  @Override
  public final boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    checkNotNull(unit, "unit");
    final long startNanos = time.nanoTime();
    final long timeoutNanos = unit.toNanos(timeout);
    boolean result = self.awaitTermination(timeout, unit);
    if (result && shutdownSubmitterAndWorker) {
      long remainingNanos = max(time.nanoTime() - startNanos, 0);
      result = submitter.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS);
      if (result) {
        if (worker != null) {
          remainingNanos = max(time.nanoTime() - startNanos, 0);
          result = worker.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS);
        }
      }
    }
    return result;
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final <T> Future<T> submit(final Callable<T> task) throws RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().submit(task);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final <T> Future<T> submit(final Runnable task, final T result) throws RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().submit(task, result);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final Future<?> submit(final Runnable task) throws RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().submit(task);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException, RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().invokeAll(tasks);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
      throws InterruptedException, RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().invokeAll(tasks, timeout, unit);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException, RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().invokeAny(tasks);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException, RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    return getWorker().invokeAny(tasks, timeout, unit);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public final void execute(final Runnable task) throws RejectedExecutionException {
    ensureActiveAndAllThreadsStarted();
    getWorker().execute(task);
  }

  private final void ensureActiveAndAllThreadsStarted() throws RejectedExecutionException {
    if (isShutdown()) {
      throw new RejectedExecutionException();
    }
    if (startAllThreads.get() && startAllThreads.compareAndSet(true, false)) {
      startAllThreads(submitter, 1);
      startAllThreads(worker, workerThreadsCount);
    }
  }

  private static final ScheduledExecutorService createSubmitter(@Nullable final ThreadFactory threadFactory, final boolean prestartThreads) {
    final ScheduledThreadPoolExecutor result = new ScheduledThreadPoolExecutor(1, ensureThreadFactory(threadFactory, "submitter-", 1));
    result.setMaximumPoolSize(1);
    result.setRemoveOnCancelPolicy(true);
    result.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    result.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    if (prestartThreads) {
      result.prestartAllCoreThreads();
    }
    return result;
  }

  @Nullable
  private static final ExecutorService createWorker(
      @Nullable final ThreadFactory threadFactory,
      final int threadsCount,
      final boolean prestartThreads) {
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || threadsCount >= 0;
    @Nullable
    final ThreadPoolExecutor result;
    if (threadsCount == 0) {
      result = null;
    } else {
      result = new ThreadPoolExecutor(
          threadsCount,
          threadsCount,
          0L,
          TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          ensureThreadFactory(threadFactory, "worker-", threadsCount));
      if (prestartThreads) {
        result.prestartAllCoreThreads();
      }
    }
    return result;
  }

  private static final ThreadFactory ensureThreadFactory(
      @Nullable final ThreadFactory threadFactory, @Nullable final String namePrefix, final int maxThreads) {
    return new BoundedThreadFactory(
        threadFactory == null
            ? namePrefix == null ? Executors.defaultThreadFactory() : new NamePrefixThreadFactory(Executors.defaultThreadFactory(), namePrefix)
            : threadFactory,
        maxThreads);
  }

  /**
   * This method must only be called for {@link ThreadPoolExecutor}
   * that was constructed by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}, i.e. was not received from somewhere.
   */
  private static final void startAllThreads(final ExecutorService executor, final int threadsCount) {
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || executor instanceof ThreadPoolExecutor;
    final ThreadPoolExecutor ex = (ThreadPoolExecutor)executor;
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ex.getMaximumPoolSize() == ex.getCorePoolSize();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ex.getCorePoolSize() == threadsCount;
    ex.prestartAllCoreThreads();
  }

  private static final int checkThreadsCountPositive(final int threadsCount) {
    checkArgument(threadsCount > 0, "threadsCount", "Must be positive");
    return threadsCount;
  }
}