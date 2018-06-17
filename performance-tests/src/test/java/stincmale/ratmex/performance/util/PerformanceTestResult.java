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

package stincmale.ratmex.performance.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.XYStyler;
import org.openjdk.jmh.annotations.Mode;
import stincmale.ratmex.doc.Nullable;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static stincmale.ratmex.performance.util.Utils.format;

public final class PerformanceTestResult extends AbstractPerformanceTestResult {
  @Nullable
  private SortedMap<String, SortedMap<Mode, SortedMap<Integer, BenchmarkResult>>> benchmark_mode_numberOfThreads_result;
  @Nullable
  private String environmentDescription;

  public static final Collection<PerformanceTestResult> load(final Predicate<String> testIdPredicate) {
    final Path directoryPath = new PerformanceTestResult("").getDirectoryPath();
    final Collection<PerformanceTestResult> result = new ArrayList<>();
    try (final Stream<Path> filePaths = Files.list(directoryPath)) {
      filePaths.forEach(filePath -> {
        @Nullable
        final Path fName = filePath.getFileName();
        if (fName != null) {
          final String fileName = fName.toString();
          final int jsonExtensionIdx = fileName.lastIndexOf(".json");
          if (jsonExtensionIdx > 0) {
            final String testId = fileName.substring(0, jsonExtensionIdx);
            if (testIdPredicate.test(testId)) {
              try {
                final PerformanceTestResult ptr = new PerformanceTestResult(testId).load();
                result.add(ptr);
                System.out.println(format("Loaded performance test result with id=%s", ptr.getTestId()));
              } catch (final RuntimeException e) {
                System.out.println(format("Failed to load performance test result from %s", filePath));
              }
            }
          }
        }
      });
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  public static final Optional<AggregatePerformanceTestResult> createAggregate(
      final String aggregateTestId, final Collection<PerformanceTestResult> ptrs) {
    final SortedMap<String, SortedMap<String, SortedMap<Mode, SortedMap<Integer, BenchmarkResult>>>>
        testId_benchmark_mode_numberOfThreads_result = new TreeMap<>();
    ptrs.forEach(ptr -> {
      ptr.ensureLoaded();
      testId_benchmark_mode_numberOfThreads_result.put(ptr.getTestId(), ptr.benchmark_mode_numberOfThreads_result);
    });
    @Nullable
    final String environmentDescription = ptrs.stream()
        .findAny()
        .map(ptr -> ptr.environmentDescription)
        .orElse(null);
    return (ptrs.isEmpty() || environmentDescription == null)
        ? Optional.empty()
        : Optional.of(new AggregatePerformanceTestResult(aggregateTestId, testId_benchmark_mode_numberOfThreads_result, environmentDescription));
  }

  public PerformanceTestResult(final String testId) {
    super(testId);
    benchmark_mode_numberOfThreads_result = null;
  }

  /**
   * @return {@code this}.
   */
  public final PerformanceTestResult load() {
    if (!isLoaded()) {
      try (final JsonReader jsonReader = Json.createReader(new InputStreamReader(
          Files.newInputStream(getDataFilePath(), READ),
          StandardCharsets.UTF_8))) {
        final SortedMap<String, SortedMap<Mode, SortedMap<Integer, BenchmarkResult>>> benchmark_mode_numberOfThreads_result = new TreeMap<>();
        jsonReader.readArray()
            .stream()
            .map(JsonValue::asJsonObject)
            .map(jsonObj -> new BenchmarkResult(getTestId(), jsonObj))
            .forEach(br -> benchmark_mode_numberOfThreads_result.compute(br.benchmark, (k1, v1) -> {
              final SortedMap<Mode, SortedMap<Integer, BenchmarkResult>> mode_numberOfThreads_result = v1 == null ? new TreeMap<>() : v1;
              mode_numberOfThreads_result.compute(br.mode, (k2, v2) -> {
                final SortedMap<Integer, BenchmarkResult> numberOfThreads_result = v2 == null ? new TreeMap<>() : v2;
                numberOfThreads_result.compute(br.numberOfThreads, (k3, v3) -> {
                  final BenchmarkResult result;
                  if (v3 == null) {
                    result = br;
                  } else {
                    throw new RuntimeException(format("Duplicate %s and %s", v3, br));
                  }
                  return result;
                });
                return numberOfThreads_result;
              });
              return mode_numberOfThreads_result;
            }));
        this.benchmark_mode_numberOfThreads_result = benchmark_mode_numberOfThreads_result;
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
      try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
          Files.newInputStream(getEnvironmentDescriptionFilePath(), READ),
          StandardCharsets.UTF_8))) {
        environmentDescription = reader.readLine();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
    return this;
  }

  public final void save(
      final Mode mode,
      final String chartTitle,
      final String xAxisTitle,
      final String yAxisTitle,
      @Nullable final String yAxisDecimalPattern,
      @Nullable final Map<String, Function<XYSeries, XYSeries>> benchmarkSeriesProcessors) {
    ensureLoaded();
    final XYChart chart = createChart(
        mode,
        format("%s, %s", chartTitle, environmentDescription),
        xAxisTitle,
        yAxisTitle,
        yAxisDecimalPattern);
    if (benchmarkSeriesProcessors == null) {
      addSeries(chart, mode, benchmarkSeriesProcessors, null);
    } else {//add specially processed series the last
      addSeries(chart, mode, benchmarkSeriesProcessors,
          benchmarkSeriesProcessors == null ? null : benchmark -> !benchmarkSeriesProcessors.containsKey(benchmark));
      addSeries(chart, mode, benchmarkSeriesProcessors,
          benchmarkSeriesProcessors == null ? null : benchmarkSeriesProcessors::containsKey);
    }
    try (final OutputStream outputStream =
        Files.newOutputStream(getDirectoryPath().resolve(format("%s-%s.png", getTestId(), mode.shortLabel())), CREATE, WRITE, TRUNCATE_EXISTING)) {
      BitmapEncoder.saveBitmap(chart, outputStream, BitmapFormat.PNG);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println(format("Saved performance test chart with id=%s, mode=%s", getTestId(), mode));
  }

  private final XYChart createChart(
      final Mode mode,
      final String chartTitle,
      final String xAxisTitle,
      final String yAxisTitle,
      @Nullable final String yAxisDecimalPattern) {
    final XYChart chart = new XYChartBuilder()
        .theme(ChartTheme.Matlab)
        .width(1920)
        .height(1080)
        .title(chartTitle)
        .xAxisTitle(xAxisTitle)
        .yAxisTitle(yAxisTitle)
        .build();
    final XYStyler styler = chart.getStyler();
    styler.setLocale(Locale.ROOT);
    styler.setChartTitleVisible(true);
    styler.setLegendPosition(LegendPosition.OutsideE);
    styler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
    styler.setAntiAlias(true);
    styler.setMarkerSize(8);
    styler.setXAxisMin(1d);
    styler.setXAxisMax(getXAxisMax(mode));
    styler.setYAxisMin(0d);
    if (yAxisDecimalPattern != null) {
      styler.setYAxisDecimalPattern(yAxisDecimalPattern);
    }
    return chart;
  }

  private final void addSeries(
      final XYChart chart,
      final Mode targetMode,
      @Nullable final Map<String, Function<XYSeries, XYSeries>> benchmarkSeriesProcessors,
      @Nullable final Predicate<String> filterBenchmarks) {
    benchmark_mode_numberOfThreads_result.forEach((benchmark, mode_numberOfThreads_result) -> {
      if (filterBenchmarks == null || filterBenchmarks.test(benchmark)) {
        mode_numberOfThreads_result.forEach((mode, numberOfThreads_result) -> {
          if (mode == targetMode) {
            final double[] xData = new double[numberOfThreads_result.size()];
            final double[] yData = new double[numberOfThreads_result.size()];
            final double[] errorBars = new double[numberOfThreads_result.size()];
            int idx = 0;
            for (final Entry<Integer, BenchmarkResult> entry : numberOfThreads_result.entrySet()) {
              final BenchmarkResult br = entry.getValue();
              xData[idx] = br.numberOfThreads;
              yData[idx] = br.score;
              errorBars[idx] = Double.isNaN(br.error) ? 0 : br.error;
              idx++;
            }
            final XYSeries series = chart.addSeries(benchmark, xData, yData, errorBars);
            series.setXYSeriesRenderStyle(getSeriesRenderStyle(benchmark, targetMode));
            @Nullable
            final Function<XYSeries, XYSeries> seriesProcessor = benchmarkSeriesProcessors == null ? null : benchmarkSeriesProcessors.get(benchmark);
            if (seriesProcessor != null) {
              seriesProcessor.apply(series);
            }
          }
        });
      }
    });
  }

  private final double getXAxisMax(final Mode mode) {
    return benchmark_mode_numberOfThreads_result.values()
        .stream()
        .map(mode_numberOfThreads_result -> mode_numberOfThreads_result.get(mode))
        .filter(Objects::nonNull)
        .mapToInt(SortedMap::lastKey)
        .max()
        .orElse(1);
  }

  private final XYSeriesRenderStyle getSeriesRenderStyle(final String benchmark, final Mode mode) {
    final int maxNumberOfSeriesWithDifferentNumberOfThreads =
        benchmark_mode_numberOfThreads_result.getOrDefault(benchmark, Collections.emptySortedMap())
            .getOrDefault(mode, Collections.emptySortedMap())
            .keySet()
            .size();
    return maxNumberOfSeriesWithDifferentNumberOfThreads > 1 ? XYSeriesRenderStyle.Line : XYSeriesRenderStyle.Scatter;
  }

  private final void ensureLoaded() {
    if (!isLoaded()) {
      throw new IllegalStateException("Data has not been loaded");
    }
  }

  private final boolean isLoaded() {
    return benchmark_mode_numberOfThreads_result != null && environmentDescription != null;
  }

  static final class BenchmarkResult {
    public final String testId;
    public final String benchmark;
    public final Mode mode;
    public final int numberOfThreads;
    public final double score;
    public final double error;
    public final String unit;

    private BenchmarkResult(
        final String testId,
        final String benchmark,
        final Mode mode,
        final int numberOfThreads,
        final double score,
        final double error,
        final String unit) {
      this.testId = testId;
      this.benchmark = benchmark;
      this.mode = mode;
      this.numberOfThreads = numberOfThreads;
      this.score = score;
      this.error = error;
      this.unit = unit;
    }

    private BenchmarkResult(final String testId, final JsonObject jsonObj) {
      this(
          testId,
          getName(jsonObj),
          Mode.deepValueOf(jsonObj.getString("mode")),
          jsonObj.getInt("threads"),
          getDouble(jsonObj.getJsonObject("primaryMetric")
              .get("score")),
          getDouble(jsonObj.getJsonObject("primaryMetric")
              .get("scoreError")),
          jsonObj.getJsonObject("primaryMetric")
              .getString("scoreUnit"));
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() +
          "{testId=" + testId +
          ", benchmark=" + benchmark +
          ", mode=" + mode +
          ", numberOfThreads=" + numberOfThreads +
          ", score=" + score +
          ", error=" + error +
          ", unit=" + unit +
          '}';
    }

    private static final String getName(final JsonObject jsonObj) {
      final String fullName = jsonObj.getString("benchmark");
      return fullName.substring(fullName.lastIndexOf('.') + 1);
    }

    private static final double getDouble(final JsonValue jsonVal) {
      final double result;
      if (jsonVal.getValueType() == ValueType.NUMBER) {
        result = ((JsonNumber)jsonVal).doubleValue();
      } else if (jsonVal.getValueType() == ValueType.NULL) {
        result = Double.NaN;
      } else {//ValueType.STRING
        result = Double.parseDouble(((JsonString)jsonVal).getString());
      }
      return result;
    }
  }
}