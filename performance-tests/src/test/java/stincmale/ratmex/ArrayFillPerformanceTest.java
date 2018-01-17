package stincmale.ratmex;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Tag(PerformanceTestTag.VALUE)
public class ArrayFillPerformanceTest {
  private static final int ARRAY_SIZE = 50_000;
  private static final int ARRAY_ITERATIONS = 24_000;
  private static final long[] clearArr = new long[ARRAY_ITERATIONS];

  public ArrayFillPerformanceTest() {
  }

  @Test
  public void serial_throughput_baseline() throws RunnerException {
    new Runner(new OptionsBuilder()
            .jvmArgsPrepend("-server")
            .jvmArgsAppend("-disableassertions")
            .syncIterations(true)
            .shouldFailOnError(true)
            .shouldDoGC(false)
            .timeout(milliseconds(10_000))
            .warmupTime(milliseconds(750))
            .warmupIterations(3)
            .measurementTime(milliseconds(1000))
            .measurementIterations(3)
            .forks(3)
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*")
        .threads(1)
        .build())
        .run();
  }

  @Benchmark
  public void forLoop(BenchmarkState state, final Blackhole bh) {
    final int fromIdx = ThreadLocalRandom.current().nextInt(0, state.arr.length / 2 - 1);
    for (int i = fromIdx; i < ARRAY_ITERATIONS; i++) {
      state.arr[i] = 0;
    }
  }

  @Benchmark
  public void fill(BenchmarkState state, final Blackhole bh) {
    final int fromIdx = ThreadLocalRandom.current().nextInt(0, state.arr.length / 2 - 1);
    Arrays.fill(state.arr, fromIdx, fromIdx + ARRAY_ITERATIONS, 0);
  }

  @Benchmark
  public void arraycopy(BenchmarkState state, final Blackhole bh) {
    final int fromIdx = ThreadLocalRandom.current().nextInt(0, state.arr.length / 2 - 1);
    System.arraycopy(clearArr, 0, state.arr, fromIdx, ARRAY_ITERATIONS);
  }

  @State(Scope.Thread)
  public static class BenchmarkState {
    long[] arr;

    public BenchmarkState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      arr = new long[ARRAY_SIZE];
    }
  }
}