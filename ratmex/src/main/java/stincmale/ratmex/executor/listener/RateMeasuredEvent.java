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
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.RateMeasuringExecutorService;
import stincmale.ratmex.executor.ScheduledTaskConfig;
import stincmale.ratmex.meter.RateMeterReading;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 */
@NotThreadSafe
public class RateMeasuredEvent {
  private final Rate targetRate;
  private final RateMeterReading completionRate;

  /**
   * @param targetRate The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be {@code null}.
   * @param completionRate The current completion rate of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be {@code null}.
   */
  public RateMeasuredEvent(final Rate targetRate, final RateMeterReading completionRate) {
    checkNotNull(targetRate, "targetRate");
    checkNotNull(completionRate, "completionRate");
    this.targetRate = targetRate;
    this.completionRate = completionRate;
  }

  /**
   * @return The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final Rate getTargetRate() {
    return targetRate;
  }

  /**
   * @return The current completion rate of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getCompletionRate() {
    return completionRate;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + targetRate +
        ", completionRate=" + completionRate +
        '}';
  }
}