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

import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import stincmale.ratmex.meter.RateMeter;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateException} if notices that the target rate is not respected
 * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
 * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
 *
 * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which this listener can react to.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type of {@link ConcurrentRateMeterStats} that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@NotThreadSafe
public class DefaultSubmitterWorkerRateListener<E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS extends ConcurrentRateMeterStats>
    extends DefaultRateListener<E> {
  private static final DefaultSubmitterWorkerRateListener<?, ?, ?> instance = new DefaultSubmitterWorkerRateListener<>();

  protected DefaultSubmitterWorkerRateListener() {
  }

  /**
   * @param <E> See {@link DefaultSubmitterWorkerRateListener}.
   * @param <SRS> See {@link DefaultSubmitterWorkerRateListener}.
   * @param <WRS> See {@link DefaultSubmitterWorkerRateListener}.
   *
   * @return A default {@link RateListener} for {@link SubmitterWorkerRateMeasuredEvent}.
   */
  @SuppressWarnings("unchecked")
  public static <E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS extends ConcurrentRateMeterStats>
  DefaultSubmitterWorkerRateListener<E, SRS, WRS> defaultSubmitterWorkerRateListenerInstance() {
    return (DefaultSubmitterWorkerRateListener<E, SRS, WRS>)instance;
  }

  /**
   * @throws RateException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected
   * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
   * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
   * @throws CorrectnessException If {@link ConcurrentRateMeterStats#incorrectlyRegisteredTickEventsCount()}
   * from {@link SubmitterWorkerRateMeasuredEvent#getWorkerRateMeterStats()} is greater than 0.
   */
  @Override
  public boolean onMeasurement(final E e) throws RateException, CorrectnessException {
    super.onMeasurement(e);
    final Rate targetRate = e.getTargetRate();
    if (targetRate.compareTo(e.getSubmissionRate()) != 0) {
      throw new RateException("The target rate was violated by the submission rate. ", e.getTargetRate(), e.getSubmissionRate()
          .getValueDouble());
    }
    if (targetRate.compareTo(e.getCompletionRate()) != 0) {
      throw new RateException("The target rate was violated by the completion rate. ", e.getTargetRate(), e.getCompletionRate()
          .getValueDouble());
    }
    e.getWorkerRateMeterStats()
      .map(ConcurrentRateMeterStats::incorrectlyRegisteredTickEventsCount)
      .ifPresent(incorrectlyRegisteredTickEventsCount -> {
        if (incorrectlyRegisteredTickEventsCount > 0) {
          throw new CorrectnessException(format("Worker rate meter failed to accurately register ticks. " +
            "incorrectlyRegisteredTickEventsCount={}, completionRate={}", incorrectlyRegisteredTickEventsCount, e.getCompletionRate()
                .getValueDouble()));
        }
      });
    return true;
  }
}