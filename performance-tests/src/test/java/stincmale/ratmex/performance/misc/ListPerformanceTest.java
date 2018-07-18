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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

/**
 * <pre>{@code
 * Benchmark                                          (size)   Mode  Cnt      Score     Error   Units
 * ListPerformanceTest.arrayListAdd                    10  thrpt   80  13565.997 ±  52.647  ops/ms
 * ListPerformanceTest.arrayListAdd                  1000  thrpt   80    129.276 ±   0.543  ops/ms
 * ListPerformanceTest.arrayListAdd                 10000  thrpt   80     20.771 ±   0.695  ops/ms
 * ListPerformanceTest.arrayListAdd                100000  thrpt   80      2.177 ±   0.020  ops/ms
 * ListPerformanceTest.arrayListEnsureCapacityAdd      10  thrpt   80  19140.346 ± 333.376  ops/ms
 * ListPerformanceTest.arrayListEnsureCapacityAdd    1000  thrpt   80    306.700 ±   1.532  ops/ms
 * ListPerformanceTest.arrayListEnsureCapacityAdd   10000  thrpt   80     27.262 ±   0.751  ops/ms
 * ListPerformanceTest.arrayListEnsureCapacityAdd  100000  thrpt   80      2.621 ±   0.031  ops/ms
 * ListPerformanceTest.arrayListIterate                10  thrpt   80  60346.684 ± 302.753  ops/ms
 * ListPerformanceTest.arrayListIterate              1000  thrpt   80   3732.739 ±  32.308  ops/ms
 * ListPerformanceTest.arrayListIterate             10000  thrpt   80    405.475 ±   1.190  ops/ms
 * ListPerformanceTest.arrayListIterate            100000  thrpt   80     41.210 ±   0.149  ops/ms
 * ListPerformanceTest.linkedListAdd                   10  thrpt   80  12414.910 ±  57.732  ops/ms
 * ListPerformanceTest.linkedListAdd                 1000  thrpt   80    133.345 ±   0.630  ops/ms
 * ListPerformanceTest.linkedListAdd                10000  thrpt   80     17.742 ±   0.182  ops/ms
 * ListPerformanceTest.linkedListAdd               100000  thrpt   80      1.735 ±   0.021  ops/ms
 * ListPerformanceTest.linkedListIterate               10  thrpt   80  59613.872 ± 262.505  ops/ms
 * ListPerformanceTest.linkedListIterate             1000  thrpt   80    535.774 ±   7.280  ops/ms
 * ListPerformanceTest.linkedListIterate            10000  thrpt   80     29.623 ±   0.254  ops/ms
 * ListPerformanceTest.linkedListIterate           100000  thrpt   80      4.481 ±   0.385  ops/ms
 * }</pre>
 */
@Disabled
@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_CLASS)
public class ListPerformanceTest {
  private static final int[] sizes = {10, 1000, 10_000, 100_000};
  private static final Map<Integer, ArrayList<Object>> arrayLists;
  private static final Map<Integer, LinkedList<Object>> linkedLists;

  static {
    arrayLists = stream(sizes)
        .mapToObj(size -> new SimpleImmutableEntry<>(size, createList(size, ArrayList::new)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    linkedLists = stream(sizes)
      .mapToObj(size -> new SimpleImmutableEntry<>(size, createList(size, LinkedList::new)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public ListPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(JmhOptions.includingClass(getClass())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .build())
        .run();
  }

  @Benchmark
  public ArrayList<Object> arrayListAdd(final ThreadState state) {
    return arrayListAdd(state.size);
  }

  @Benchmark
  public ArrayList<Object> arrayListEnsureCapacityAdd(final ThreadState state) {
    return arrayListAddEnsureCapacity(state.size);
  }

  @Benchmark
  public boolean arrayListIterate(final ThreadState state) {
    return arrayListIterate(arrayLists.get(state.size));
  }

  @Benchmark
  public LinkedList<Object> linkedListAdd(final ThreadState state) {
    return linkedListAdd(state.size);
  }

  @Benchmark
  public boolean linkedListIterate(final ThreadState state) {
    return linkedListIterate(linkedLists.get(state.size));
  }

  @State(Scope.Thread)
  public static class ThreadState {
    @Param({"10", "1000", "10000", "100000"})
    private int size;

    public ThreadState() {
    }
  }

  private final ArrayList<Object> arrayListAdd(final int size) {
    final ArrayList<Object> list = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      list.add(new Object());
    }
    return list;
  }

  private final ArrayList<Object> arrayListAddEnsureCapacity(final int size) {
    final ArrayList<Object> list = new ArrayList<>();
    list.ensureCapacity(size);
    for (int i = 0; i < size; i++) {
      list.add(new Object());
    }
    return list;
  }

  private boolean arrayListIterate(final ArrayList<Object> list) {
    final Object last = list.get(list.size() - 1);
    boolean result = false;
    for (final Object o : list) {
      if (o == last) {
        result = true;
      }
    }
    return result;
  }

  private final LinkedList<Object> linkedListAdd(final int size) {
    final LinkedList<Object> list = new LinkedList<>();
    for (int i = 0; i < size; i++) {
      list.add(new Object());
    }
    return list;
  }

  private boolean linkedListIterate(final LinkedList<Object> list) {
    final Object last = list.getLast();
    boolean result = false;
    for (final Object o : list) {
      if (o == last) {
        result = true;
      }
    }
    return result;
  }

  private static final <L extends List<Object>> L createList(final int size, final Supplier<L> listFactory) {
    return IntStream.range(0, size)
      .mapToObj(i -> new Object())
      .collect(Collectors.toCollection(listFactory));
  }
}