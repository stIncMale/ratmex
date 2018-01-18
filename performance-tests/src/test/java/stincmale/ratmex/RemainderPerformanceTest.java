package stincmale.ratmex;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.util.JmhOptions;
import stincmale.ratmex.util.PerformanceTestTag;

/**
 * <pre>{@code
 * Benchmark                                Mode  Cnt  Score   Error   Units
 * RemainderPerformanceTest.bitwise        thrpt   15  2.627 ± 0.020  ops/ms
 * RemainderPerformanceTest.remainder      thrpt   15  1.268 ± 0.019  ops/ms
 * RemainderPerformanceTest.remainderPow2  thrpt   15  2.220 ± 0.022  ops/ms
 * }</pre>
 */
@Tag(PerformanceTestTag.VALUE)
public class RemainderPerformanceTest {//TODO get rid of cycles
  private static final int ITERATIONS = 1_000_000;
  private static final long DENOMINATOR_POW2 = BigInteger.TWO.pow(10)
      .longValueExact();
  private static final long BITWISE_DENOMINATOR_POW2 = DENOMINATOR_POW2 - 1;
  private static final long DENOMINATOR = BITWISE_DENOMINATOR_POW2 - 1;

  public RemainderPerformanceTest() {
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
  public long remainder() {
    long s = 0;
    for (int i = 1; i < ITERATIONS; i++) {
      s += i % DENOMINATOR;
    }
    return s;
  }

  @Benchmark
  public long remainderPow2() {
    long s = 0;
    for (int i = 1; i < ITERATIONS; i++) {
      s += i % DENOMINATOR_POW2;
    }
    return s;
  }

  @Benchmark
  public long bitwise() {
    long s = 0;
    for (int i = 1; i < ITERATIONS; i++) {
      s += i & BITWISE_DENOMINATOR_POW2;
    }
    return s;
  }
}