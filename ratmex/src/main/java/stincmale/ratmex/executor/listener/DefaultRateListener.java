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

import stincmale.ratmex.doc.NotThreadSafe;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateException} if notices that the target rate is not respected.
 *
 * @param <E> A type of a {@link RateMeasuredEvent} which this listener can react to.
 */
@NotThreadSafe
public class DefaultRateListener<E extends RateMeasuredEvent> implements RateListener<E> {
  private static final DefaultRateListener<RateMeasuredEvent> instance = new DefaultRateListener<>();

  protected DefaultRateListener() {
  }

  /**
   * @param <E> See {@link DefaultRateListener}.
   *
   * @return A default {@link RateListener}.
   */
  @SuppressWarnings("unchecked")
  public static <E extends RateMeasuredEvent> DefaultRateListener<E> defaultRateListenerInstance() {
    return (DefaultRateListener<E>)instance;
  }

  /**
   * @throws RateException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected.
   */
  @Override
  public boolean onMeasurement(final E e) throws RateException {
    if (e.getTargetRate()
        .compareTo(e.getCompletionRate()) != 0) {
      throw new RateException(
          "The completion rate violated the target rate. ",
          e.getTargetRate(),
          e.getCompletionRate()
              .getValueDouble());
    }
    return true;
  }
}