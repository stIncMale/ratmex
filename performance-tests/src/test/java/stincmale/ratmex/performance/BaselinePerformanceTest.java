package stincmale.ratmex.performance;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.performance.util.JmhOptions;
import stincmale.ratmex.performance.util.JmhPerformanceTestResult;
import stincmale.ratmex.performance.util.PerformanceTestResult;
import stincmale.ratmex.performance.util.PerformanceTestTag;
import stincmale.ratmex.performance.util.Utils;

@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_METHOD)
public class BaselinePerformanceTest {
  public BaselinePerformanceTest() {
  }

  @Test
  public void run() {
    for (int numberOfThreads : JmhOptions.numbersOfThreads) {
      testResults.addAll(runThroughput(numberOfThreads));
      testResults.addAll(runLatency(numberOfThreads));
    }
  }

  @Benchmark
  public long systemNanoTime() {
    return System.nanoTime();
  }

  @Benchmark
  public long granularitySystemNanoTime(final ThreadState state) {
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

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private final AtomicLong atomicLong = new AtomicLong();
    private final AtomicLongArray atomicLongArray = new AtomicLongArray(1);

    public BenchmarkState() {
    }
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private long lastNanoTime = System.nanoTime();

    public ThreadState() {
    }
  }

  private final Collection<RunResult> runThroughput(final int numberOfThreads) {
    final Collection<RunResult> runResults;
    try {
      runResults = new Runner(JmhOptions.get()
          .include(getClass().getName() + ".(?!granularity).*")
          .mode(Mode.Throughput)
          .timeUnit(TimeUnit.MICROSECONDS)
          .threads(numberOfThreads)
          .build())
          .run();
    } catch (final RunnerException e) {
      throw new RuntimeException(e);
    }
    return runResults;
  }

  private final Collection<RunResult> runLatency(final int numberOfThreads) {
    final Collection<RunResult> runResults;
    try {
      runResults = new Runner(JmhOptions.get()
          .include(numberOfThreads == 1
              ? getClass().getName() + ".*"
              : getClass().getName() + ".(?!granularity).*")
          .mode(Mode.AverageTime)
          .timeUnit(TimeUnit.NANOSECONDS)
          .threads(numberOfThreads)
          .build())
          .run();
    } catch (final RunnerException e) {
      throw new RuntimeException(e);
    }
    return runResults;
  }

  private static Collection<RunResult> testResults;

  @BeforeAll
  public static final void beforeAll() {
    testResults = new CopyOnWriteArrayList<>();
  }

  @AfterAll
  public static final void afterAll() {
    final String testId = BaselinePerformanceTest.class.getSimpleName();
    new JmhPerformanceTestResult(testId, BaselinePerformanceTest.class, testResults).save();
    if (!Utils.isHeadless()) {
      final PerformanceTestResult ptr = new PerformanceTestResult(testId, BaselinePerformanceTest.class)
          .load();
      ptr.save(Mode.Throughput, BaselinePerformanceTest.class.getSimpleName(), "number of threads", "throughput, ops/s", "#.# mln", null);
      ptr.save(Mode.AverageTime, BaselinePerformanceTest.class.getSimpleName(), "number of threads", "latency, ns", null,
          Collections.singletonMap("granularitySystemNanoTime", series -> {
            series.setMarkerColor(Color.BLACK);
            series.setLineColor(Color.BLACK);
            series.setMarker(SeriesMarkers.SQUARE);
            series.setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
            return series;
          }));
    }
  }
}