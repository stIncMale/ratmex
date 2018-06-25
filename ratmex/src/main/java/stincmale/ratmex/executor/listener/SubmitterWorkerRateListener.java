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

import stincmale.ratmex.executor.SubmitterWorkerRateMeasuringExecutorService;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import stincmale.ratmex.meter.RateMeter;

/**
 * A {@link RateListener} which can be used with {@link SubmitterWorkerRateMeasuringExecutorService}.
 *
 * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which this listener can react to.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type of {@link ConcurrentRateMeterStats} that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
public abstract class SubmitterWorkerRateListener<
    E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS extends ConcurrentRateMeterStats> implements RateListener<E> {
  protected SubmitterWorkerRateListener() {
  }
}