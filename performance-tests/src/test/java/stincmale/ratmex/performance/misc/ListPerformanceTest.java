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

package stincmale.ratmex.performance.misc;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.performance.util.JmhOptions;
import stincmale.ratmex.performance.util.PerformanceTestTag;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;

/**
 * <pre>{@code
 * Benchmark                              (listDescriptor)  (size)   Mode  Cnt      Score     Error   Units
 * ListPerformanceTest.add                      ARRAY_LIST      10  thrpt   40  14522.597 ±  85.230  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST     100  thrpt   40   1471.285 ±  18.341  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST    1000  thrpt   40    128.794 ±   0.817  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST   10000  thrpt   40     21.489 ±   0.115  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST  100000  thrpt   40      2.231 ±   0.014  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST      10  thrpt   40  14874.221 ± 119.266  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST     100  thrpt   40   1631.998 ±  13.034  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST    1000  thrpt   40    167.614 ±   0.952  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST   10000  thrpt   40     18.147 ±   0.188  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST  100000  thrpt   40      1.789 ±   0.016  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY      10  thrpt   40  12945.806 ±  61.106  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY     100  thrpt   40   1277.771 ±   6.958  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY    1000  thrpt   40    147.486 ±   5.359  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY   10000  thrpt   40     30.944 ±   0.211  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY  100000  thrpt   40      2.767 ±   0.019  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST      10  thrpt   40  49500.498 ± 272.120  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST     100  thrpt   40  16678.608 ± 199.563  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST    1000  thrpt   40   2080.560 ±  32.715  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST   10000  thrpt   40    221.332 ±   0.898  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST  100000  thrpt   40     22.379 ±   0.104  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST      10  thrpt   40  50945.654 ± 215.243  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST     100  thrpt   40   7093.456 ±  28.562  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST    1000  thrpt   40    584.081 ±   2.632  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST   10000  thrpt   40     27.847 ±   0.107  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST  100000  thrpt   40      2.702 ±   0.022  ops/ms
 * ListPerformanceTest.iterate  ARRAY_LIST_ENSURE_CAPACITY      10  thrpt   40  49457.966 ± 314.636  ops/ms
 * ListPerformanceTest.iterate  ARRAY_LIST_ENSURE_CAPACITY     100  thrpt   40  16683.608 ± 198.955  ops/ms
 * ListPerformanceTest.iterate  ARRAY_LIST_ENSURE_CAPACITY    1000  thrpt   40   2070.582 ±  28.876  ops/ms
 * ListPerformanceTest.iterate  ARRAY_LIST_ENSURE_CAPACITY   10000  thrpt   40    221.445 ±   1.084  ops/ms
 * ListPerformanceTest.iterate  ARRAY_LIST_ENSURE_CAPACITY  100000  thrpt   40     22.386 ±   0.096  ops/ms
 * }</pre>
 */
@Disabled
@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_CLASS)
public class ListPerformanceTest {
  public ListPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(JmhOptions.includingClass(getClass())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .shouldDoGC(true)
        .build())
        .run();
  }

  @Benchmark
  public List<Object> add(final ThreadState state) {
    final int size = state.size;
    final List<Object> list = state.listDescriptor.create(size);
    for (int i = 0; i < size; i++) {
      list.add(new Object());
    }
    return list;
  }

  @Benchmark
  public boolean iterate(final ThreadState state) {
    boolean result = false;
    final List<Object> list = state.listDescriptor.get(state.size);
    for (final Object o : list) {
      if (o == list) {
        result = true;
      }
    }
    return result;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private static final Map<Integer, ArrayList<Object>> arrayLists;
    private static final Map<Integer, LinkedList<Object>> linkedLists;

    static {
      final int[] sizes;
      try {
        sizes = stream(ThreadState.class.getDeclaredField("size")
            .getAnnotation(Param.class)
            .value())
            .mapToInt(Integer::parseInt)
            .toArray();
      } catch (final NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      arrayLists = stream(sizes)
        .mapToObj(size -> new SimpleImmutableEntry<>(size, createList(size, ArrayList::new)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      linkedLists = stream(sizes)
        .mapToObj(size -> new SimpleImmutableEntry<>(size, createList(size, LinkedList::new)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Param({"5", "50", "500", "5000", "50000"})
    private int size;
    @Param({"ARRAY_LIST", "LINKED_LIST", "ARRAY_LIST_ENSURE_CAPACITY"})
    private ListDescriptor listDescriptor;

    public ThreadState() {
    }

    private static final <L extends List<Object>> L createList(final int size, final Supplier<L> listFactory) {
      return range(0, size)
        .mapToObj(i -> new Object())
        .collect(Collectors.toCollection(listFactory));
    }

    public enum ListDescriptor {
      ARRAY_LIST(size -> new ArrayList<>(), ThreadState.arrayLists::get),
      ARRAY_LIST_ENSURE_CAPACITY(
        size -> {
          final ArrayList<Object> result = new ArrayList<>();
          result.ensureCapacity(size);
          return result;
        },
        ARRAY_LIST.supplierOfPreconstructed),
      LINKED_LIST(size -> new LinkedList<>(), ThreadState.linkedLists::get);

      private final Function<Integer, List<Object>> supplierOfNew;
      private final Function<Integer, List<Object>> supplierOfPreconstructed;

      ListDescriptor(final Function<Integer, List<Object>> supplierOfNew, final Function<Integer, List<Object>> supplierOfPreconstructed) {
        this.supplierOfNew = supplierOfNew;
        this.supplierOfPreconstructed = supplierOfPreconstructed;
      }

      private List<Object> create(final int size) {
        return supplierOfNew.apply(size);
      }

      private final List<Object> get(final int size) {
        return supplierOfPreconstructed.apply(size);
      }
    }
  }
}