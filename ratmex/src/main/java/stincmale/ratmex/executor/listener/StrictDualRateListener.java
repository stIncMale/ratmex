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

import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import stincmale.ratmex.meter.RateMeter;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * An implementation of {@link RateListener} which does not allow any deviations from correctness
 * (see {@link #onMeasurement(DualRateMeasuredEvent)}).
 *
 * @param <E> See {@link RateListener}.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type of {@link ConcurrentRateMeterStats} that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@ThreadSafe
public final class StrictDualRateListener<
    E extends DualRateMeasuredEvent<? extends SRS, ? extends WRS>, SRS, WRS extends ConcurrentRateMeterStats> implements RateListener<E> {
  private static final StrictDualRateListener<?, ?, ?> instance = new StrictDualRateListener<>();

  private StrictDualRateListener() {
  }

  /**
   * Always returns the same instance.
   *
   * @param <E> See {@link StrictDualRateListener}.
   * @param <SRS> See {@link StrictDualRateListener}.
   * @param <WRS> See {@link StrictDualRateListener}.
   *
   * @return An instance of {@link StrictDualRateListener}.
   */
  @SuppressWarnings("unchecked")
  public static final <E extends DualRateMeasuredEvent<? extends SRS, ? extends WRS>, SRS, WRS extends ConcurrentRateMeterStats>
  StrictDualRateListener<E, SRS, WRS> instance() {
    return (StrictDualRateListener<E, SRS, WRS>)instance;
  }

  /**
   * @throws RateException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected
   * by either a {@linkplain DualRateMeasuredEvent#getSubmissionRate() submitter}
   * or a {@linkplain DualRateMeasuredEvent#getCompletionRate() worker}.
   * @throws CorrectnessException If {@link ConcurrentRateMeterStats#incorrectlyRegisteredTickEventsCount()}
   * from {@link DualRateMeasuredEvent#getWorkerRateMeterStats()} is greater than 0.
   */
  @Override
  public final boolean onMeasurement(final E e) throws RateException, CorrectnessException {
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
        .map(WRS::incorrectlyRegisteredTickEventsCount)
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