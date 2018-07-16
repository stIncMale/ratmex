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

package stincmale.ratmex.executor.listener;

import java.util.Optional;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.executor.AbstractSubmitterWorkerRateMeasuringExecutorService;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.config.ScheduledTaskConfig;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 *{@link RateMeasuredEvent} used by {@link AbstractSubmitterWorkerRateMeasuringExecutorService}.
 *
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@NotThreadSafe
public class SubmitterWorkerRateMeasuredEvent<SRS, WRS> extends RateMeasuredEvent {
  private final RateMeterReading submissionRate;
  private final RateMeterReading completionRate;
  @Nullable
  private final SRS submitterRateMeterStats;
  @Nullable
  private final WRS workerRateMeterStats;

  /**
   * @param targetRate See {@link RateMeasuredEvent#RateMeasuredEvent(Rate)}.
   * @param submissionRate The current submission rate of the
   * {@linkplain AbstractSubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be null.
   * @param completionRate The current completion rate of the
   * {@linkplain AbstractSubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be null.
   * @param submitterRateMeterStats {@linkplain RateMeter#stats() Statistics} of the submitter {@link RateMeter}.
   * @param workerRateMeterStats {@linkplain RateMeter#stats() Statistics} of the worker {@link RateMeter}.
   */
  public SubmitterWorkerRateMeasuredEvent(
      final Rate targetRate,
      final RateMeterReading submissionRate,
      final RateMeterReading completionRate,
      @Nullable
      final SRS submitterRateMeterStats,
      @Nullable
      final WRS workerRateMeterStats) {
    super(targetRate);
    checkNotNull(submissionRate, "submissionRate");
    checkNotNull(completionRate, "completionRate");
    this.submissionRate = submissionRate;
    this.completionRate = completionRate;
    this.submitterRateMeterStats = submitterRateMeterStats;
    this.workerRateMeterStats = workerRateMeterStats;
  }

  /**
   * @return The current submission rate of the
   * {@linkplain AbstractSubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getSubmissionRate() {
    return submissionRate;
  }

  /**
   * @return The current completion rate of the
   * {@linkplain AbstractSubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getCompletionRate() {
    return submissionRate;
  }

  /**
   * @return {@linkplain RateMeter#stats() Statistics} of the submitter {@link RateMeter}.
   */
  public final Optional<SRS> getSubmitterRateMeterStats() {
    return Optional.ofNullable(submitterRateMeterStats);
  }

  /**
   * @return {@linkplain RateMeter#stats() Statistics} of the worker {@link RateMeter}.
   */
  public final Optional<WRS >getWorkerRateMeterStats() {
    return Optional.ofNullable(workerRateMeterStats);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + getTargetRate() +
        ", submissionRate=" + submissionRate +
        ", completionRate=" + completionRate +
        ", submitterRateMeterStats=" + submitterRateMeterStats +
        ", workerRateMeterStats=" + workerRateMeterStats +
        '}';
  }
}