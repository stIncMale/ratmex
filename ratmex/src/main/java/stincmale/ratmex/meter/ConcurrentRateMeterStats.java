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

package stincmale.ratmex.meter;

import stincmale.ratmex.doc.ThreadSafe;

/**
 * Stats gathered by a thread-safe implementation of {@link RateMeter} which implements {@link RateMeter#tick(long, long)} in a relaxed way,
 * that allows come ticks to be registered at an incorrect instant for example.
 */
@ThreadSafe
public interface ConcurrentRateMeterStats {
  /**
   * @return The number of situations in which
   * {@link RateMeter} might have {@linkplain RateMeter#tick(long, long) registered ticks} at an incorrect instant, or just lost ticks.
   *
   * @see ConcurrentRateMeterConfig.Mode#RELAXED_TICKS
   */
  long incorrectlyRegisteredTickEventsCount();
}