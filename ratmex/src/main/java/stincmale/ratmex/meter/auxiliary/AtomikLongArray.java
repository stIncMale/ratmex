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

import java.util.concurrent.atomic.AtomicLongArray;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;

/**
 * This concurrent implementation of {@link LongArray} is just a wrapper over an {@link AtomicLongArray}.
 * It is named {@link AtomikLongArray} with {@code 'k'} in order to avoid a name clash.
 */
@ThreadSafe
public final class AtomikLongArray implements LongArray {
  final AtomicLongArray array;

  /**
   * @param length The length of the new array. Must be positive.
   */
  public AtomikLongArray(final int length) {
    checkArgument(length > 0, "length", "Must be positive");
    this.array = new AtomicLongArray(length);
  }

  /**
   * @return See {@link AtomicLongArray#length()}.
   */
  @Override
  public final int length() {
    return array.length();
  }

  /**
   * @return See {@link AtomicLongArray#set(int, long)}.
   */
  @Override
  public final void set(final int idx, final long value) {
    array.set(idx, value);
  }

  /**
   * @return See {@link AtomicLongArray#get(int)}.
   */
  @Override
  public final long get(int idx) {
    return array.get(idx);
  }

  /**
   * @return See {@link AtomicLongArray#getAndAdd(int, long)}.
   */
  @Override
  public final void add(final int idx, final long delta) {
    array.getAndAdd(idx, delta);
  }

  /**
   * @return See {@link AtomicLongArray#toString()}.
   */
  @Override
  public final String toString() {
    return array.toString();
  }
}