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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.executor.config.ScheduledTaskConfig;
import stincmale.ratmex.executor.listener.RateListener;
import stincmale.ratmex.executor.listener.RateMeasuredEvent;

/**
 * A rate-measuring executor service which not only executes tasks with a fixed {@link Rate rate},
 * but also measures the actual rate completion of the tasks
 * and allows one to {@linkplain RateListener monitor the rate and react} if the executor has failed to conform to the target rate.
 * <p>
 * <b>The reasoning behind {@link RateMeasuringExecutorService}</b><br>
 * The functionality described by this interface cannot be directly obtained from
 * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}, which says the following regarding the task being scheduled:
 * <i>"If any execution of this task takes longer than its period, then subsequent executions may start late, but will not concurrently execute"</i>.
 * This tells us that:
 * <ul>
 * <li>{@link ScheduledExecutorService} is allowed to execute tasks with a lower rate than the target (and there is no easy way to check).</li>
 * <li>{@link ScheduledExecutorService} executes a scheduled task serially, which means that you cannot easily benefit from multithreading,
 * and the rate is heavily limited by the time the task takes to complete.</li>
 * </ul>
 * {@link RateMeasuringExecutorService} overcomes both of the above points.
 *
 * @param <C> A type of scheduled task config used in {@link #scheduleAtFixedRate(Runnable, Rate, C)}.
 * @param <E> A type of container with data provided to a {@linkplain ScheduledTaskConfig#getRateListenerSupplier() rate listener}.
 */
@ThreadSafe
public interface RateMeasuringExecutorService<C extends ScheduledTaskConfig<? extends E>, E extends RateMeasuredEvent> extends ExecutorService,
  AutoCloseable {
  /**
   * Schedules a {@code task} to be executed with a fixed {@code rate}.
   * <p>
   * The repeated execution of the {@code task} continues indefinitely until
   * one of the following exceptional completions occur:
   * <ul>
   * <li>The task is {@linkplain Future#cancel(boolean) explicitly cancelled} via the returned future.</li>
   * <li>The executor {@linkplain #isTerminated() terminates}, also resulting in task {@linkplain Future#cancel(boolean) cancellation}.</li>
   * <li>An execution of the task throws a {@link RuntimeException}.
   * In this case calling {@link Future#get() get()} on the returned future will throw {@link ExecutionException}.</li>
   * <li>The scheduled task {@linkplain ScheduledTaskConfig#getDuration() times out}.</li>
   * <li>The {@linkplain ScheduledTaskConfig#getRateListenerSupplier() rate listener}'s method
   * {@link RateListener#onMeasurement(RateMeasuredEvent)} returns false,
   * also resulting in task {@linkplain Future#cancel(boolean) cancellation}.</li>
   * </ul>
   * Subsequent executions are suppressed. Subsequent calls to {@link Future#isDone isDone()} on the returned future will return {@code true}.
   *
   * @param task A task to schedule for repetitive execution. Must not be {@code null}.
   * @param targetRate A target rate of completion of the task executions. Must not be {@code null}.
   * @param config An additional configuration. Must not be {@code null}.
   *
   * @return A {@link ScheduledFuture} representing pending completion of the {@code task}.
   * The future's {@link Future#get() get()} method will never return normally,
   * and will throw an exception upon task cancellation or abnormal termination of a task execution.
   *
   * @throws RejectedExecutionException If the task cannot be scheduled for execution.
   */
  ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Rate targetRate, C config) throws RejectedExecutionException;

  /**
   * This method is equivalent to calling {@link #shutdownNow()}.
   * Subclasses may extend this behaviour, but must always call {@link #shutdownNow()}.
   */
  @Override
  default void close() {
    shutdownNow();
  }
}