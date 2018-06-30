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

import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * A default implementation of {@link SubmitterWorkerRateListener}.
 *
 * @param <E> See {@link SubmitterWorkerRateListener}.
 * @param <SRS> See {@link SubmitterWorkerRateListener}.
 * @param <WRS> See {@link SubmitterWorkerRateListener}.
 */
@ThreadSafe
public class DefaultSubmitterWorkerRateListener<
    E extends SubmitterWorkerRateMeasuredEvent<? extends SRS, ? extends WRS>, SRS, WRS extends ConcurrentRateMeterStats>
    extends SubmitterWorkerRateListener<E, SRS, WRS> {
  private static final DefaultSubmitterWorkerRateListener<?, ?, ?> instance = new DefaultSubmitterWorkerRateListener<>();

  protected DefaultSubmitterWorkerRateListener() {
  }

  /**
   * Always returns the same instance.
   *
   * @param <E> See {@link DefaultSubmitterWorkerRateListener}.
   * @param <SRS> See {@link DefaultSubmitterWorkerRateListener}.
   * @param <WRS> See {@link DefaultSubmitterWorkerRateListener}.
   *
   * @return An instance of {@link DefaultSubmitterWorkerRateListener}.
   */
  @SuppressWarnings("unchecked")
  public static <E extends SubmitterWorkerRateMeasuredEvent<? extends SRS, ? extends WRS>, SRS, WRS extends ConcurrentRateMeterStats>
      DefaultSubmitterWorkerRateListener<E, SRS, WRS> instance() {
    return (DefaultSubmitterWorkerRateListener<E, SRS, WRS>)instance;
  }

  /**
   * @throws RateException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected
   * by either a {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
   * or a {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
   * @throws CorrectnessException If {@link ConcurrentRateMeterStats#incorrectlyRegisteredTickEventsCount()}
   * from {@link SubmitterWorkerRateMeasuredEvent#getWorkerRateMeterStats()} is greater than 0.
   */
  @Override
  public boolean onMeasurement(final E e) throws RateException, CorrectnessException {
    final Rate targetRate = e.getTargetRate();
    if (targetRate.compareTo(e.getSubmissionRate()) != 0) {
      throw new RateException("The target rate was violated by the submission rate. ", e.getTargetRate(), e.getSubmissionRate()
          .getValueDouble());
    }
    if (targetRate.compareTo(e.getCompletionRate()) != 0) {
      throw new RateException("The target rate was violated by the completion rate. ", e.getTargetRate(), e.getCompletionRate()
          .getValueDouble());
    }
    @Nullable
    final WRS workerRateMeterStats = e.getWorkerRateMeterStats();
    if (workerRateMeterStats != null) {
      final long incorrectlyRegisteredTickEventsCount = workerRateMeterStats.incorrectlyRegisteredTickEventsCount();
      if (incorrectlyRegisteredTickEventsCount > 0) {
        throw new CorrectnessException(format("Worker rate meter failed to accurately register ticks. " +
            "incorrectlyRegisteredTickEventsCount={}, completionRate={}", incorrectlyRegisteredTickEventsCount, e.getCompletionRate()
                .getValueDouble()));
      }
    }
    return true;
  }
}