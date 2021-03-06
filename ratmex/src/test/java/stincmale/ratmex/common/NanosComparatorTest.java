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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static stincmale.ratmex.internal.util.Utils.format;

@TestInstance(Lifecycle.PER_METHOD)
public final class NanosComparatorTest {
  private static final Sample[] samples = {
      new Sample(Long.MIN_VALUE, Long.MIN_VALUE, CompareResult.EQUAL),
      new Sample(Long.MAX_VALUE, Long.MAX_VALUE, CompareResult.EQUAL),
      new Sample(0, 0, CompareResult.EQUAL),
      new Sample(Long.MIN_VALUE + 1, Long.MIN_VALUE, CompareResult.GREATER),
      new Sample(Long.MAX_VALUE - 1, Long.MAX_VALUE, CompareResult.LOWER),
      new Sample(0, Long.MAX_VALUE, CompareResult.LOWER),
      new Sample(83147568, 83147568 + Long.MAX_VALUE, CompareResult.LOWER),
      new Sample(-2, Long.MAX_VALUE, CompareResult.GREATER),
      new Sample(-1, Long.MIN_VALUE, CompareResult.GREATER),
      new Sample(Long.MAX_VALUE - Long.MAX_VALUE / 2, Long.MIN_VALUE + Long.MAX_VALUE / 2, CompareResult.LOWER),
      new Sample(Long.MIN_VALUE, Long.MAX_VALUE, CompareResult.GREATER),
      new Sample(Long.MIN_VALUE + 1000, Long.MAX_VALUE - 1000, CompareResult.GREATER),
      new Sample(Long.MIN_VALUE, Long.MAX_VALUE - 2, CompareResult.GREATER),
      new Sample(Long.MIN_VALUE, Long.MAX_VALUE, CompareResult.GREATER),
      new Sample(-2, 5, CompareResult.LOWER),
      new Sample(5, 3, CompareResult.GREATER),
      new Sample(0, Long.MIN_VALUE, CompareResult.UNSUPPORTED),
      new Sample(-1, Long.MAX_VALUE, CompareResult.UNSUPPORTED),
      new Sample(Long.MIN_VALUE + 1, 2, CompareResult.GREATER),
      new Sample(1000, 1000 + Long.MAX_VALUE, CompareResult.LOWER),
      new Sample(-1000, -1000 + Long.MAX_VALUE, CompareResult.LOWER),
      new Sample(Long.MIN_VALUE, Long.MIN_VALUE + Long.MAX_VALUE, CompareResult.LOWER),
      new Sample(1000 - Long.MAX_VALUE, 1000, CompareResult.LOWER),
      new Sample(-1000 - Long.MAX_VALUE, -1000, CompareResult.LOWER),
      new Sample(Long.MIN_VALUE - Long.MAX_VALUE, Long.MIN_VALUE, CompareResult.LOWER),
  };

  public NanosComparatorTest() {
  }

  @Test
  public final void compare() {
    for (final Sample sample : samples) {
      assertCompare(sample.l1, sample.l2, sample.expected, NanosComparator.instance());
    }
  }

  @Test
  public final void min() {
    for (final Sample sample : samples) {
      if (sample.expected != CompareResult.UNSUPPORTED) {
        final long expected = sample.expected == CompareResult.LOWER ? sample.l1 : sample.l2;
        assertEquals(expected, NanosComparator.min(sample.l1, sample.l2));
      }
    }
  }

  @Test
  public final void max() {
    for (final Sample sample : samples) {
      if (sample.expected != CompareResult.UNSUPPORTED) {
        final long expected = sample.expected == CompareResult.LOWER ? sample.l2 : sample.l1;
        assertEquals(expected, NanosComparator.max(sample.l1, sample.l2));
      }
    }
  }

  private static final void unsafeAssertCompareSymmetry(final Long l1, final Long l2, final NanosComparator comparator) {
    assertSame(Integer.signum(comparator.compare(l1, l2)), -Integer.signum(comparator.compare(l2, l1)), format("l1 = %s, l2 = %s", l1, l2));
  }

  private static final void assertCompare(final Long l1, final Long l2, final CompareResult expected, final NanosComparator comparator) {
    if (expected != CompareResult.UNSUPPORTED) {
      unsafeAssertCompareSymmetry(l1, l2, comparator);
    }
    switch (expected) {
      case LOWER: {
        assertTrue(comparator.compare(l1, l2) < 0, format("l1 = %s, l2 = %s", l1, l2));
        break;
      }
      case GREATER: {
        assertTrue(comparator.compare(l1, l2) > 0, format("l1 = %s, l2 = %s", l1, l2));
        break;
      }
      case EQUAL: {
        assertTrue(comparator.compare(l1, l2) == 0, format("l1 = %s, l2 = %s", l1, l2));
        break;
      }
      case UNSUPPORTED: {
        boolean illegalArguments = false;
        try {
          comparator.compare(l1, l2);
        } catch (final IllegalArgumentException e) {
          illegalArguments = true;
        }
        assertTrue(illegalArguments, format("l1 = %s, l2 = %s, illegalArguments = %s", l1, l2, illegalArguments));
        break;
      }
      default: {
        throw new RuntimeException();
      }
    }
  }

  private enum CompareResult {
    LOWER,
    GREATER,
    EQUAL,
    UNSUPPORTED
  }

  private static final class Sample {
    final long l1;
    final long l2;
    final CompareResult expected;

    Sample(final long l1, final long l2, final CompareResult expected) {
      this.l1 = l1;
      this.l2 = l2;
      this.expected = expected;
    }
  }
}