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

import java.util.Arrays;
import stincmale.ratmex.doc.NotThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;

/**
 * This sequential implementation of {@link LongArray} is just a wrapper over a plain {@code long[]} array.
 */
@NotThreadSafe
public final class PlainLongArray implements LongArray {
  final long[] array;

  /**
   * @param length The length of the new array. Must be positive.
   */
  public PlainLongArray(final int length) {
    checkArgument(length > 0, "length", "Must be positive");
    this.array = new long[length];
  }

  @Override
  public final int length() {
    return array.length;
  }

  @Override
  public final void set(final int idx, final long value) {
    array[idx] = value;
  }

  @Override
  public final long get(int idx) {
    return array[idx];
  }

  @Override
  public final void add(final int idx, final long delta) {
    array[idx] += delta;
  }

  /**
   * @return A string representation of the underlying array as per {@link Arrays#toString(long[])}.
   */
  @Override
  public final String toString() {
    return Arrays.toString(array);
  }
}