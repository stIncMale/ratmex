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
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.ScheduledTaskConfig;
import stincmale.ratmex.executor.SubmitterWorkerRateMeasuringExecutorService;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A container with data provided to {@link RateListener} by {@link SubmitterWorkerRateMeasuringExecutorService}.
 *
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@NotThreadSafe
public class SubmitterWorkerRateMeasuredEvent<SRS, WRS> extends RateMeasuredEvent {
  private final RateMeterReading submissionRate;
  @Nullable
  private SRS submitterRateMeterStats;
  @Nullable
  private WRS workerRateMeterStats;

  /**
   * @param targetRate The target rate of completion of the
   * {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be {@code null}.
   * @param completionRate The current completion rate of the
   * {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * @param submissionRate The current submission rate of the
   * {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public SubmitterWorkerRateMeasuredEvent(
      final Rate targetRate,
      final RateMeterReading submissionRate,
      final RateMeterReading completionRate) {
    super(targetRate, completionRate);
    checkNotNull(submissionRate, "submissionRate");
    this.submissionRate = submissionRate;
  }

  /**
   * @return The current submission rate of the
   * {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getSubmissionRate() {
    return submissionRate;
  }

  /**
   * @return {@linkplain RateMeter#stats() Statistics} of submitter {@link RateMeter}.
   */
  public final Optional<SRS> getSubmitterRateMeterStats() {
    return Optional.ofNullable(submitterRateMeterStats);
  }

  /**
   * Sets {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
   *
   * @param submitterRateMeterStats May be null.
   *
   * @return {@code this}.
   */
  public final SubmitterWorkerRateMeasuredEvent<SRS, WRS> setSubmitterRateMeterStats(@Nullable final SRS submitterRateMeterStats) {
    this.submitterRateMeterStats = submitterRateMeterStats;
    return this;
  }

  /**
   * @return {@linkplain RateMeter#stats() Statistics} of worker {@link RateMeter}.
   */
  public final Optional<WRS> getWorkerRateMeterStats() {
    return Optional.ofNullable(workerRateMeterStats);
  }

  /**
   * Sets {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
   *
   * @param workerRateMeterStats May be null.
   *
   * @return {@code this}.
   */
  public final SubmitterWorkerRateMeasuredEvent<SRS, WRS> setWorkerRateMeterStats(final WRS workerRateMeterStats) {
    this.workerRateMeterStats = workerRateMeterStats;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + getTargetRate() +
        ", submissionRate=" + submissionRate +
        ", completionRate=" + getCompletionRate() +
        ", submitterRateMeterStats=" + submitterRateMeterStats +
        ", workerRateMeterStats=" + workerRateMeterStats +
        '}';
  }
}