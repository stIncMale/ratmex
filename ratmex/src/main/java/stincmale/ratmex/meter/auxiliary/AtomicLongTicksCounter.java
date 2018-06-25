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

package stincmale.ratmex.meter.auxiliary;

import java.util.concurrent.atomic.AtomicLong;
import stincmale.ratmex.doc.ThreadSafe;

/**
 * This {@link TicksCounter} uses {@link AtomicLong} field to store its {@linkplain #get() value}.
 */
@ThreadSafe
public final class AtomicLongTicksCounter extends AbstractTicksCounter {
  private final AtomicLong aValue;

  public AtomicLongTicksCounter(final long initialValue) {
    aValue = new AtomicLong(initialValue);
  }

  @Override
  public final void add(final long delta) {
    aValue.addAndGet(delta);
  }

  @Override
  public final long get() {
    return aValue.get();
  }
}