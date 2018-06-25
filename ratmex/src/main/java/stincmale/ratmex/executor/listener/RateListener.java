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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import stincmale.ratmex.doc.Immutable;
import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.doc.ThreadSafe;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.RateMeasuringExecutorService;
import stincmale.ratmex.executor.config.ScheduledTaskConfig;

/**
 * A listener allowing monitoring the rate and reacting if there are deviations from the {@linkplain RateMeasuredEvent#getTargetRate() target rate}.
 *
 * @param <E> A type of a {@link RateMeasuredEvent} which this listener can react to.
 */
@NotThreadSafe
public interface RateListener<E extends RateMeasuredEvent> {
  /**
   * This method is called by {@link RateMeasuringExecutorService} each time it decides to notify about the current measured data,
   * which should include current execution rate of the scheduled task.
   * This method must never be called concurrently with events generated for the same scheduled task, implementations of
   * {@link RateMeasuringExecutorService} must adhere to this constraint.
   * However, implementations of {@link RateListener} still may be made {@linkplain ThreadSafe thread-safe},
   * thus allowing reusing the same instance for different scheduled tasks.
   * <p>
   * Any {@link RuntimeException} thrown from this method causes the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled execution}
   * for which the {@code event} was generated to terminate abnormally
   * (i.e. cause method {@linkplain ScheduledFuture#get()} to throw {@link ExecutionException}
   * which provides the thrown exception via {@link ExecutionException#getCause()}).
   *
   * @param e An event with data provided by {@link RateMeasuringExecutorService}.
   * An implementation of this method must not pass on a reference to the event
   * in a way that allows accessing the event outside of this method invocation because an event may be mutable and its contents are only
   * guaranteed to stay unchanged within an invocation of this method. Primitive or {@linkplain Immutable immutable} data extracted from the event
   * can, however, be transferred anywhere.
   *
   * @return true if the {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled execution}
   * for which the event was generated must continue; otherwise it will be {@linkplain ScheduledFuture#cancel(boolean) cancelled}.
   *
   * @throws RateException Optional, may be thrown if the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected.
   * Note that implementation of this method may choose to ignore the deviation and return true
   * instead thus continuing the scheduled repetitive execution.
   * @throws CorrectnessException Optional, may be thrown if any incorrectness is detected
   * (e.g. incorrectness related to relaxed behaviour of any of the tools involved).
   */
  boolean onMeasurement(E e) throws RateException, CorrectnessException;
}