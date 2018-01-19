package stincmale.ratmex;

import java.awt.Color;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.util.JmhOptions;
import stincmale.ratmex.util.PerformanceTestTag;
import static stincmale.ratmex.util.Utils.format;
import static stincmale.ratmex.util.Utils.isHeadless;

@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_METHOD)
public class BaselinePerformanceTest {
  public BaselinePerformanceTest() {
  }

  @Test
  public void run_serial_throughput() {
    run_throughput(1);
  }

  @Test
  public void run_serial_latency() {
    run_latency(1);
  }

  @Test
  public void run_parallel4_throughput() throws RunnerException {
    run_throughput(4);
  }

  @Test
  public void run_parallel4_latency() throws RunnerException {
    run_latency(4);
  }

  @Test
  public void run_parallel8_throughput() throws RunnerException {
    run_throughput(8);
  }

  @Test
  public void run_parallel8_latency() throws RunnerException {
    run_latency(8);
  }

  @Test
  public void run_parallel16_throughput() throws RunnerException {
    run_throughput(16);
  }

  @Test
  public void run_parallel16_latency() throws RunnerException {
    run_latency(16);
  }

  @Test
  public void run_parallel32_throughput() throws RunnerException {
    run_throughput(32);
  }

  @Test
  public void run_parallel32_latency() throws RunnerException {
    run_latency(32);
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

  private final void run_throughput(final int numberOfThreads) {
    final int availableProcessors = Runtime.getRuntime()
        .availableProcessors();
    if (availableProcessors < 0) {
      System.out.println();
      System.out.println(format("The run was skipped because there are not enough processors. Required %s, available %s.",
          numberOfThreads, availableProcessors));
      System.out.println();
    } else {
      try {
        throughputRunResults.put(numberOfThreads, new Runner(JmhOptions.get()
            .include(getClass().getName() + ".(?!granularity).*")
            .mode(Mode.Throughput)
            .timeUnit(TimeUnit.MICROSECONDS)
            .threads(numberOfThreads)
            .build())
            .run());
      } catch (final RunnerException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final void run_latency(final int numberOfThreads) {
    final int availableProcessors = Runtime.getRuntime()
        .availableProcessors();
    if (availableProcessors < 0) {
      System.out.println();
      System.out.println(format("The run was skipped because there are not enough processors. Required %s, available %s.",
          numberOfThreads, availableProcessors));
      System.out.println();
    } else {
      try {
        latencyRunResults.put(numberOfThreads, new Runner(JmhOptions.get()
            .include(numberOfThreads == 1
                ? getClass().getName() + ".*"
                : getClass().getName() + ".(?!granularity).*")
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .threads(numberOfThreads)
            .build())
            .run());
      } catch (final RunnerException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static Map<Integer, Collection<RunResult>> throughputRunResults;
  private static Map<Integer, Collection<RunResult>> latencyRunResults;

  @BeforeAll
  public static final void beforeAll() {
    throughputRunResults = new TreeMap<>();
    latencyRunResults = new TreeMap<>();
  }

  @AfterAll
  public static final void afterAll() {
    final XYChart throughputChart = buildChart(regroupRunResults(throughputRunResults),
        BaselinePerformanceTest.class.getSimpleName() + ", " + environmentDescription(),
        "number of threads", "throughput, s\u207B\u00B9", 1, "### mln");
    final XYChart latencyChart = buildChart(regroupRunResults(latencyRunResults),
        BaselinePerformanceTest.class.getSimpleName() + ", " + environmentDescription(),
        "number of threads", "latency, ns", 1, null);
    if (!isHeadless()) {
      try {
        final String path = "../";
        BitmapEncoder.saveBitmap(throughputChart, path + BaselinePerformanceTest.class.getSimpleName() + "-throughput", BitmapFormat.PNG);
        BitmapEncoder.saveBitmap(latencyChart, path + BaselinePerformanceTest.class.getSimpleName() + "-latency", BitmapFormat.PNG);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      //TODO export CSV
    }
  }

  private static final String environmentDescription() {
    final int availableProcessors = Runtime.getRuntime()
        .availableProcessors();
    final String jvm = format("JVM: %s %s", System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
    final String os = format("OS: %s %s %s", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
    return format("%s, %s, processors: %s", jvm, os, availableProcessors);
  }

  private static final XYChart buildChart(
      final Map<String, Map<Integer, RunResult>> throughputResults,
      final String chartTitle,
      final String xAxisTitle,
      final String yAxisTitle,
      final double yAxisFactor,
      @Nullable final String yAxisDecimalPattern) {
    final XYChart chart = new XYChartBuilder()
        .theme(ChartTheme.Matlab)
        .width(1280)
        .height(720)
        .title(chartTitle)
        .xAxisTitle(xAxisTitle)
        .yAxisTitle(yAxisTitle)
        .build();
    final XYStyler styler = chart.getStyler();
    styler.setChartTitleVisible(true);
    styler.setLegendPosition(LegendPosition.OutsideE);
    styler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
    styler.setAntiAlias(true);
    styler.setMarkerSize(8);
    if (yAxisDecimalPattern != null) {
      styler.setYAxisDecimalPattern(yAxisDecimalPattern);
    }
    throughputResults.entrySet()
        .stream()
        .filter(benchmarkName_numberOfThreads_runResult -> !benchmarkName_numberOfThreads_runResult.getKey()
            .contains("granularity"))
        .forEach(benchmarkName_numberOfThreads_runResult -> {
          final String benchmarkName = benchmarkName_numberOfThreads_runResult.getKey();
          final Map<Integer, RunResult> numberOfThreads_runResult = benchmarkName_numberOfThreads_runResult.getValue();
          final double[] xData = new double[numberOfThreads_runResult.size()];
          final double[] yData = new double[numberOfThreads_runResult.size()];
          final double[] errorBars = new double[numberOfThreads_runResult.size()];
          int idx = 0;
          for (final Entry<Integer, RunResult> entry : numberOfThreads_runResult.entrySet()) {
            xData[idx] = entry.getKey();
            yData[idx] = entry.getValue()
                .getPrimaryResult()
                .getScore() * yAxisFactor;
            final double rawError = entry.getValue()
                .getPrimaryResult()
                .getScoreError();
            errorBars[idx] = Double.isNaN(rawError) ? 0 : rawError * yAxisFactor;
            idx++;
          }
          chart.addSeries(benchmarkName, xData, yData, errorBars);
        });
    throughputResults.entrySet()
        .stream()
        .filter(benchmarkName_numberOfThreads_runResult -> benchmarkName_numberOfThreads_runResult.getKey()
            .contains("granularity"))
        .findAny()
        .ifPresent(benchmarkName_numberOfThreads_runResult -> {
          final String benchmarkName = benchmarkName_numberOfThreads_runResult.getKey();
          final Map<Integer, RunResult> numberOfThreads_runResult = benchmarkName_numberOfThreads_runResult.getValue();
          final double[] xData = new double[numberOfThreads_runResult.size()];
          final double[] yData = new double[numberOfThreads_runResult.size()];
          final double[] errorBars = new double[numberOfThreads_runResult.size()];
          int idx = 0;
          for (final Entry<Integer, RunResult> entry : numberOfThreads_runResult.entrySet()) {
            xData[idx] = entry.getKey();
            yData[idx] = entry.getValue()
                .getPrimaryResult()
                .getScore() * yAxisFactor;
            final double rawError = entry.getValue()
                .getPrimaryResult()
                .getScoreError();
            errorBars[idx] = Double.isNaN(rawError) ? 0 : rawError * yAxisFactor;
            idx++;
          }
          final XYSeries series = chart.addSeries(benchmarkName, xData, yData, errorBars);
          series.setMarkerColor(Color.BLACK);
          series.setLineColor(Color.BLACK);
          series.setMarker(SeriesMarkers.SQUARE);
          series.setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
        });
    return chart;
  }

  private static final Map<String, Map<Integer, RunResult>> regroupRunResults(final Map<Integer, Collection<RunResult>> runResults) {
    return runResults.entrySet()
        .stream()
        .flatMap(numberOfThreads_runResults -> numberOfThreads_runResults.getValue()
            .stream()
            .map(runResult -> {
              final String benchmarkName = runResult.getParams()
                  .getBenchmark();
              final String shortBenchmarkName = benchmarkName.substring(benchmarkName.lastIndexOf('.') + 1);
              return new SimpleImmutableEntry<>(shortBenchmarkName, new SimpleImmutableEntry<>(numberOfThreads_runResults.getKey(), runResult));
            }))
        .collect(
            TreeMap::new,
            (result, benchmarkName_numberOfThreads_runResult) -> {
              final String benchmarkName = benchmarkName_numberOfThreads_runResult.getKey();
              result.compute(benchmarkName, (k, v) -> {
                final Map<Integer, RunResult> res = v == null ? new TreeMap<>() : v;
                res.put(
                    benchmarkName_numberOfThreads_runResult.getValue()
                        .getKey(),
                    benchmarkName_numberOfThreads_runResult.getValue()
                        .getValue());
                return res;
              });
            },
            (map1, map2) -> map2.forEach((benchmarkName, subMap) -> map1.merge(
                benchmarkName,
                subMap,
                (subMap1, subMap2) -> {
                  final Map<Integer, RunResult> res = new TreeMap<>();
                  res.putAll(subMap1);
                  subMap2.forEach((numberOfThreads, runResult) ->
                      res.merge(numberOfThreads, runResult, (runResult1, runResult2) -> {
                        throw new RuntimeException(format("Conflict: benchmarkName=%s, numberOfThreads=%s, runResult1=%s, runResult2=%s",
                            benchmarkName, numberOfThreads, runResult1, runResult2));
                      }));
                  return res;
                })));
  }
}