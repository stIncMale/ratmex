package stincmale.ratmex.performance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.knowm.xchart.XYSeries;
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

@Disabled
@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_CLASS)
public class BaselinePerformanceTest {
  public BaselinePerformanceTest() {
  }

  @Test
  public void run() {
    final int numberOfAvailableProcessors = Runtime.getRuntime()
        .availableProcessors();
    final Collection<RunResult> runResults = new ArrayList<>();
    for (int numberOfThreads : JmhOptions.numbersOfThreads) {
      runResults.addAll(runThroughput(numberOfThreads));
      if (numberOfThreads <= numberOfAvailableProcessors) {
        runResults.addAll(runLatency(numberOfThreads));
      }
    }
    new JmhPerformanceTestResult(getTestId(), BaselinePerformanceTest.class, runResults).save();
  }

  @Benchmark
  public long systemNanoTime() {
    return System.nanoTime();
  }

  @Benchmark
  public long systemNanoTimeGranularity(final ThreadState state) {
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
          .include(getClass().getName() + ".(?!systemNanoTimeGranularity).*")
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
              : getClass().getName() + ".(?!systemNanoTimeGranularity).*")
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

  @AfterAll
  public static final void afterAll() {
    System.out.println();
    generateCharts(Collections.singleton(new PerformanceTestResult(getTestId(), BaselinePerformanceTest.class).load()));
  }

  public static final void generateCharts(final Collection<? extends PerformanceTestResult> loadedPtrs) {
    if (!Utils.isHeadless()) {
      final String testId = getTestId();
      final Map<String, Function<XYSeries, XYSeries>> benchmarkSeriesProcessors = new TreeMap<>();
      {
        benchmarkSeriesProcessors.put("atomicLongGetAndAdd", series -> (XYSeries)series.setMarker(SeriesMarkers.SQUARE));
        benchmarkSeriesProcessors.put("atomicLongArrayGetAndAdd", series -> (XYSeries)series.setMarker(SeriesMarkers.SQUARE));
        benchmarkSeriesProcessors.put("systemNanoTime", series -> (XYSeries)series.setMarker(SeriesMarkers.CIRCLE));
        benchmarkSeriesProcessors.put("systemNanoTimeGranularity", series -> {
          series.setMarkerColor(Color.BLACK);
          series.setLineColor(Color.BLACK);
          series.setMarker(SeriesMarkers.DIAMOND);
          return series;
        });
      }
      loadedPtrs.stream()
          .filter(ptr -> testId.equals(ptr.getTestId()))
          .findAny()
          .ifPresent(ptr -> {
            ptr.save(
                Mode.Throughput,
                BaselinePerformanceTest.class.getSimpleName(),
                "number of threads",
                "throughput, ops/s",
                "#.# mln",
                benchmarkSeriesProcessors);
            ptr.save(
                Mode.AverageTime,
                BaselinePerformanceTest.class.getSimpleName(),
                "number of threads",
                "latency, ns",
                null,
                benchmarkSeriesProcessors);
          });
    }
  }

  private static final String getTestId() {
    return BaselinePerformanceTest.class.getSimpleName();
  }
}