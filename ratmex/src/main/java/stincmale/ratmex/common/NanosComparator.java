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

package stincmale.ratmex.common;

import java.util.Comparator;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * A {@link Comparator} that compares nanoseconds according to {@link System#nanoTime()} specification.
 */
@ThreadSafe
public final class NanosComparator implements Comparator<Long> {
  private static final NanosComparator instance = new NanosComparator();

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link NanosComparator}.
   */
  public static final NanosComparator instance() {
    return instance;
  }

  /**
   * Acts like {@link #compare(Long, Long)} but for primitive values.
   *
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  public static final int compare(final long l1, final long l2) throws IllegalArgumentException {
    final long diff = l1 - l2;
    if (diff == Long.MIN_VALUE) {
      throw new IllegalArgumentException(format("Nanos %s and %s aren't comparable", l1, l2));
    }
    final int result;
    if (diff < 0) {
      result = -1;
    } else if (diff > 0) {
      result = 1;
    } else {
      result = 0;
    }
    return result;
  }

  /**
   * Returns the smaller (see {@link #compare(long, long)}) of two nanosecond values.
   * If the arguments have the same value, the result is that same value.
   *
   * @param l1 An argument.
   * @param l2 Another argument.
   *
   * @return The smaller of two nanosecond values.
   *
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  public static final long min(final long l1, final long l2) throws IllegalArgumentException {
    return compare(l1, l2) <= 0 ? l1 : l2;
  }

  /**
   * Returns the greater (see {@link #compare(long, long)}) of two nanosecond values.
   * If the arguments have the same value, the result is that same value.
   *
   * @param l1 An argument.
   * @param l2 Another argument.
   *
   * @return The larger of two nanosecond values.
   *
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  public static final long max(final long l1, final long l2) throws IllegalArgumentException {
    return compare(l1, l2) >= 0 ? l1 : l2;
  }

  /**
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  @Override
  public final int compare(final Long l1, final Long l2) throws IllegalArgumentException {
    checkNotNull(l1, "o1");
    checkNotNull(l2, "o2");
    return compare(l1.longValue(), l2.longValue());
  }

  private NanosComparator() {
  }
}