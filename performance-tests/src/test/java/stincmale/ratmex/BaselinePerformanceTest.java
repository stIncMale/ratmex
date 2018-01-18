package stincmale.ratmex;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.util.JmhOptions;
import stincmale.ratmex.util.PerformanceTestTag;

@Tag(PerformanceTestTag.VALUE)
public class BaselinePerformanceTest {

  public BaselinePerformanceTest() {
  }

  @Test
  public void run_serial_throughput() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_granularity).*")
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .build())
        .run();
  }

  @Test
  public void run_serial_latency() throws RunnerException {
    new Runner(JmhOptions.includingClass(getClass())
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .build())
        .run();
  }

  @Test
  public void run_parallel4_throughput() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void run_parallel4_latency() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void run_parallel8_throughput() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .threads(8)
        .build())
        .run();
  }

  @Test
  public void run_parallel8_latency() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .threads(8)
        .build())
        .run();
  }

  @Test
  public void run_parallel16_throughput() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .threads(16)
        .build())
        .run();
  }

  @Test
  public void run_parallel16_latency() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .threads(16)
        .build())
        .run();
  }

  @Test
  public void run_parallel32_throughput() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .threads(32)
        .build())
        .run();
  }

  @Test
  public void run_parallel32_latency() throws RunnerException {
    new Runner(JmhOptions.get()
        .include(getClass().getName() + ".(?!serial_).*")
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .threads(32)
        .build())
        .run();
  }

  @Benchmark
  public void serial_empty() {
  }

  @Benchmark
  public void blackholeConsume(final Blackhole bh) {
    bh.consume(0);
  }

  @Benchmark
  public long systemNanoTime() {
    return System.nanoTime();
  }

  @Benchmark
  public long serial_granularitySystemNanoTime(final ThreadState state) {
    long nanoTime;
    do {
      nanoTime = System.nanoTime();
    } while (nanoTime == state.lastNanoTime);
    state.lastNanoTime = nanoTime;
    return nanoTime;
  }

  @Benchmark
  public void atomicLongGetAndAdd(final BenchmarkState state) {
    state.atomicLong.getAndAdd(1);
  }

  @Benchmark
  public void atomicLongArrayGetAndAdd(final BenchmarkState state) {
    state.atomicLongArray.getAndAdd(0, 1);
  }

  @Benchmark
  public void longAdderAdd(final BenchmarkState state) {
    state.longAdder.add(1);
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private final AtomicLong atomicLong = new AtomicLong();
    private final AtomicLongArray atomicLongArray = new AtomicLongArray(1);
    private final LongAdder longAdder = new LongAdder();

    public BenchmarkState() {
    }
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private long lastNanoTime = System.nanoTime();

    public ThreadState() {
    }
  }
}