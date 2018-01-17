package stincmale.ratmex;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Tag(PerformanceTestTag.VALUE)
public class RemainderPerformanceTest {
  private static final int ITERATIONS = 1_000_000;
  private static final long DENOMINATOR_POW2 = BigInteger.TWO.pow(10).longValueExact();
  private static final long BITWISE_DENOMINATOR_POW2 = DENOMINATOR_POW2 - 1;
  private static final long DENOMINATOR = BITWISE_DENOMINATOR_POW2 - 1;

  public RemainderPerformanceTest() {
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
        .timeUnit(TimeUnit.MICROSECONDS)
        .include(getClass().getName() + ".*")
        .threads(1)
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