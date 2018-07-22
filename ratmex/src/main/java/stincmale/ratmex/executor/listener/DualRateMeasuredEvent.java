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
import stincmale.ratmex.executor.AbstractDualRateMeasuringExecutorService;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.config.ScheduledTaskConfig;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 *{@link RateMeasuredEvent} used by {@link AbstractDualRateMeasuringExecutorService}.
 *
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@NotThreadSafe
public final class DualRateMeasuredEvent<SRS, WRS> implements RateMeasuredEvent {
  private final Rate targetRate;
  private final RateMeterReading submissionRate;
  private final RateMeterReading completionRate;
  private final Optional<SRS> submitterRateMeterStats;
  private final Optional<WRS> workerRateMeterStats;

  /**
   * @param targetRate See {@link #getTargetRate()}.
   * @param submissionRate The current submission rate of the
   * {@linkplain AbstractDualRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be null.
   * @param completionRate The current completion rate of the
   * {@linkplain AbstractDualRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be null.
   * @param submitterRateMeterStats {@linkplain RateMeter#stats() Statistics} of the submitter {@link RateMeter}.
   * @param workerRateMeterStats {@linkplain RateMeter#stats() Statistics} of the worker {@link RateMeter}.
   */
  public DualRateMeasuredEvent(
      final Rate targetRate,
      final RateMeterReading submissionRate,
      final RateMeterReading completionRate,
      @Nullable
      final SRS submitterRateMeterStats,
      @Nullable
      final WRS workerRateMeterStats) {
    checkNotNull(targetRate, "targetRate");
    checkNotNull(submissionRate, "submissionRate");
    checkNotNull(completionRate, "completionRate");
    this.targetRate = targetRate;
    this.submissionRate = submissionRate;
    this.completionRate = completionRate;
    this.submitterRateMeterStats = Optional.ofNullable(submitterRateMeterStats);
    this.workerRateMeterStats = Optional.ofNullable(workerRateMeterStats);
  }

  @Override
  public final Rate getTargetRate() {
    return targetRate;
  }

  /**
   * @return The current submission rate of the
   * {@linkplain AbstractDualRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getSubmissionRate() {
    return submissionRate;
  }

  /**
   * @return The current completion rate of the
   * {@linkplain AbstractDualRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getCompletionRate() {
    return submissionRate;
  }

  /**
   * @return {@linkplain RateMeter#stats() Statistics} of the submitter {@link RateMeter}.
   */
  public final Optional<SRS> getSubmitterRateMeterStats() {
    return submitterRateMeterStats;
  }

  /**
   * @return {@linkplain RateMeter#stats() Statistics} of the worker {@link RateMeter}.
   */
  public final Optional<WRS >getWorkerRateMeterStats() {
    return workerRateMeterStats;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + targetRate +
        ", submissionRate=" + submissionRate +
        ", completionRate=" + completionRate +
        ", submitterRateMeterStats=" + submitterRateMeterStats +
        ", workerRateMeterStats=" + workerRateMeterStats +
        '}';
  }
}