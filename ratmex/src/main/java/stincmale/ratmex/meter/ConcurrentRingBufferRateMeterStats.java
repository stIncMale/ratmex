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

import java.util.concurrent.atomic.LongAdder;
import stincmale.ratmex.doc.ThreadSafe;

@ThreadSafe
final class ConcurrentRingBufferRateMeterStats implements ConcurrentRateMeterStats {
  private final LongAdder incorrectlyRegisteredTickEventsCounter;

  ConcurrentRingBufferRateMeterStats() {
    incorrectlyRegisteredTickEventsCounter = new LongAdder();
  }

  @Override
  public final long incorrectlyRegisteredTickEventsCount() {
    return incorrectlyRegisteredTickEventsCounter.sum();
  }

  final void registerIncorrectlyRegisteredTicksEvent() {
    incorrectlyRegisteredTickEventsCounter.increment();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{incorrectlyRegisteredTickEventsCounter=" + incorrectlyRegisteredTickEventsCounter +
        '}';
  }
}