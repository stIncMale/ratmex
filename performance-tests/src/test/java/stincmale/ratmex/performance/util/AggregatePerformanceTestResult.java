package stincmale.ratmex.performance.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.openjdk.jmh.annotations.Mode;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.performance.util.PerformanceTestResult.BenchmarkResult;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static stincmale.ratmex.performance.util.Utils.format;

public final class AggregatePerformanceTestResult extends AbstractPerformanceTestResult {
  private final SortedMap<String, SortedMap<String, SortedMap<Mode, SortedMap<Integer, BenchmarkResult>>>>
      testId_benchmark_mode_numberOfThreads_result;
  private final String environmentDescription;

  AggregatePerformanceTestResult(
      final String testId,
      final SortedMap<String, SortedMap<String, SortedMap<Mode, SortedMap<Integer, BenchmarkResult>>>>
          testId_benchmark_mode_numberOfThreads_result,
      final String environmentDescription) {
    super(testId);
    this.testId_benchmark_mode_numberOfThreads_result = testId_benchmark_mode_numberOfThreads_result;
    this.environmentDescription = environmentDescription;
  }

  public final void save(
      final Mode mode,
      final int numberOfThreads,
      final String chartTitle,
      final String yAxisTitle,
      @Nullable final String yAxisDecimalPattern) {
    final CategoryChart chart = createChart(
        format("%s, %s", chartTitle, environmentDescription),
        yAxisTitle,
        yAxisDecimalPattern);
    addSeries(chart, mode, numberOfThreads);
    if (!chart.getSeriesMap()
        .isEmpty()) {
      try (final OutputStream outputStream =
          Files.newOutputStream(getDirectoryPath().resolve(format("%s-%s-%s.png", getTestId(), numberOfThreads, mode.shortLabel())), CREATE, WRITE,
              TRUNCATE_EXISTING)) {
        BitmapEncoder.saveBitmap(chart, outputStream, BitmapFormat.PNG);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
      System.out.println(format("Saved aggregate performance test chart with id=%s, mode=%s", getTestId(), mode));
    }
  }

  private final CategoryChart createChart(
      final String chartTitle,
      final String yAxisTitle,
      @Nullable final String yAxisDecimalPattern) {
    final CategoryChart chart = new CategoryChartBuilder()
        .theme(ChartTheme.Matlab)
        .width(1920)
        .height(1080)
        .title(chartTitle)
        .yAxisTitle(yAxisTitle)
        .build();
    final CategoryStyler styler = chart.getStyler();
    styler.setLocale(Locale.ROOT);
    styler.setChartTitleVisible(true);
    styler.setLegendPosition(LegendPosition.OutsideE);
    styler.setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Bar);
    styler.setAntiAlias(true);
    styler.setMarkerSize(8);
    styler.setYAxisMin(0d);
    if (yAxisDecimalPattern != null) {
      styler.setYAxisDecimalPattern(yAxisDecimalPattern);
    }
    return chart;
  }

  private final void addSeries(
      final CategoryChart chart,
      final Mode targetMode,
      final int numberOfThreads) {
    testId_benchmark_mode_numberOfThreads_result.forEach((testId, benchmark_mode_numberOfThreads_result) -> {
      final List<? extends Entry<String, BenchmarkResult>> data = benchmark_mode_numberOfThreads_result.entrySet()
          .stream()
          .map(entry_benchmark_mode_numberOfThreads_result -> {
            @Nullable
            final SortedMap<Integer, BenchmarkResult> numberOfThreads_result = entry_benchmark_mode_numberOfThreads_result.getValue()
                .get(targetMode);
            @Nullable
            final BenchmarkResult benchmarkResult = numberOfThreads_result == null ? null : numberOfThreads_result.get(numberOfThreads);
            return benchmarkResult == null
                ? null
                : new SimpleImmutableEntry<>(entry_benchmark_mode_numberOfThreads_result.getKey(), benchmarkResult);
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toCollection(ArrayList::new));
      if (!data.isEmpty()) {
        final CategorySeries series = chart.addSeries(
            testId,
            data.stream()
                .map((Function<Entry<String, BenchmarkResult>, String>)Entry::getKey)
                .collect(Collectors.toList()),
            data.stream()
                .map(entry_benchmark_result -> entry_benchmark_result.getValue().score)
                .collect(Collectors.toList()),
            data.stream()
                .map(entry_benchmark_result -> {
                  final double error = entry_benchmark_result.getValue().error;
                  return Double.isNaN(error) ? 0 : error;
                })
                .collect(Collectors.toList()));
      }
    });
  }
}