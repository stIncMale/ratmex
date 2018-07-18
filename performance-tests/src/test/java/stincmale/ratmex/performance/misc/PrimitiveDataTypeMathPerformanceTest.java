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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.performance.util.JmhOptions;
import stincmale.ratmex.performance.util.PerformanceTestTag;

/**
 * <pre>{@code
 * Benchmark                         Mode  Cnt    Score   Error   Units
 * PrimitiveDataTypeMathPerformanceTest.divInt    thrpt   80   45.208 ± 0.332  ops/us
 * PrimitiveDataTypeMathPerformanceTest.divLong   thrpt   80   12.736 ± 0.535  ops/us
 * PrimitiveDataTypeMathPerformanceTest.multInt   thrpt   80  194.092 ± 4.246  ops/us
 * PrimitiveDataTypeMathPerformanceTest.multLong  thrpt   80  210.017 ± 3.077  ops/us
 * PrimitiveDataTypeMathPerformanceTest.sumInt    thrpt   80  199.729 ± 3.220  ops/us
 * PrimitiveDataTypeMathPerformanceTest.sumLong   thrpt   80  196.766 ± 3.090  ops/us
 * }</pre>
 */
@Disabled
@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_CLASS)
public class PrimitiveDataTypeMathPerformanceTest {
  private static final int[] operandsInt = {-123456789, -1234567, -12345, -123, 123, 12345, 1234567, 123456789};
  private static final long[] operandsLong = {-123456789, -1234567, -12345, -123, 123, 12345, 1234567, 123456789};

  public PrimitiveDataTypeMathPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(JmhOptions.includingClass(getClass())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MICROSECONDS)
        .build())
        .run();
  }

  @Benchmark
  public int sumInt(final ThreadState state) {
    final int l = --state.counterInt;
    int result = 0;
    for (int r : operandsInt) {
      result += l + r;
    }
    return result;
  }

  @Benchmark
  public long sumLong(final ThreadState state) {
    final long l = --state.counterLong;
    long result = 0;
    for (long r : operandsLong) {
      result += l + r;
    }
    return result;
  }

  @Benchmark
  public int multInt(final ThreadState state) {
    final int l = --state.counterInt;
    int result = 0;
    for (int r : operandsInt) {
      result += l * r;
    }
    return result;
  }

  @Benchmark
  public long multLong(final ThreadState state) {
    final long l = --state.counterLong;
    long result = 0;
    for (long r : operandsLong) {
      result += l * r;
    }
    return result;
  }

  @Benchmark
  public int divInt(final ThreadState state) {
    final int l = --state.counterInt;
    int result = 0;
    for (int r : operandsInt) {
      result += l / r;
    }
    return result;
  }

  @Benchmark
  public long divLong(final ThreadState state) {
    final long l = --state.counterLong;
    long result = 0;
    for (long r : operandsLong) {
      result += l / r;
    }
    return result;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private int counterInt;
    private long counterLong;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counterInt = Integer.MAX_VALUE;
      counterLong = Long.MAX_VALUE;
    }
  }
}