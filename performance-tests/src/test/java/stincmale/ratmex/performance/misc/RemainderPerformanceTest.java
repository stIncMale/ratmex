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

import java.math.BigInteger;
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
 * Benchmark                                   Mode  Cnt    Score    Error   Units
 * RemainderPerformanceTest.bitwiseRemainder  thrpt   15  387.239 ± 15.700  ops/us
 * RemainderPerformanceTest.remainder         thrpt   15  300.034 ±  9.423  ops/us
 * RemainderPerformanceTest.remainderPow2     thrpt   15  343.877 ±  9.826  ops/us
 * }</pre>
 */
@Disabled
@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_CLASS)
public class RemainderPerformanceTest {
  private static final long denominatorPow2 = BigInteger.valueOf(2)
      .pow(10)
      .longValueExact();
  private static final long bitwiseDenominatorPow2 = denominatorPow2 - 1;
  private static final long denominator = bitwiseDenominatorPow2 - 1;

  public RemainderPerformanceTest() {
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
  public long remainder(final ThreadState state) {
    return (state.counter++) % denominator;
  }

  @Benchmark
  public long remainderPow2(final ThreadState state) {
    return (state.counter++) % denominatorPow2;
  }

  @Benchmark
  public long bitwiseRemainder(final ThreadState state) {
    return (state.counter++) & bitwiseDenominatorPow2;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private long counter;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counter = 0;
    }
  }
}