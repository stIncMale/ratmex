package stincmale.ratmex;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.util.JmhOptions;
import stincmale.ratmex.util.PerformanceTestTag;

/**
 * <pre>{@code
 * Benchmark                                  Mode  Cnt    Score   Error   Units
 * ArrayFillPerformanceTest.forLoop          thrpt   15  236.610 ± 4.036  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy  thrpt   15  105.536 ± 1.747  ops/ms
 * }</pre>
 */
@Disabled
@Tag(PerformanceTestTag.VALUE)
public class ArrayFillPerformanceTest {
  private static final int ARRAY_SIZE = 20_000;
  private static final long[] arrFilledWithZeros = new long[ARRAY_SIZE];

  static {//just to ignore IDE warning regarding arrFilledWithZeros
    for (int i = 0; i < arrFilledWithZeros.length; i++) {
      arrFilledWithZeros[i] = 0;
    }
  }

  public ArrayFillPerformanceTest() {
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
  public void forLoop(final ThreadState state) {
    for (int i = 0; i < state.arr.length; i++) {
      state.arr[i] = 0;
    }
  }

  @Benchmark
  public void systemArrayCopy(final ThreadState state) {
    System.arraycopy(arrFilledWithZeros, 0, state.arr, 0, state.arr.length);
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private final long[] arr = new long[ARRAY_SIZE];

    public ThreadState() {
    }
  }
}