package stincmale.ratmex;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import stincmale.ratmex.util.JmhOptions;
import stincmale.ratmex.util.PerformanceTestTag;

public class XChartTest {
  public XChartTest() {
  }

  @Test
  public void run() {
    XYChart chart = new XYChartBuilder().width(600).height(500).title("Gaussian Blobs").xAxisTitle("X").yAxisTitle("Y").build();
    chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
    chart.getStyler().setChartTitleVisible(false);
    chart.getStyler().setLegendPosition(LegendPosition.InsideSW);
    chart.getStyler().setMarkerSize(16);
    // Series
    XYSeries series = chart.addSeries("Gaussian Blob 2", new double[] {1, 2, 3, 4, 5}, new double[] {1, 2, 4, 8, 16}, new double[] {0.1, 0.04, 0.2, 0.15, 0.01});
    series.setMarker(SeriesMarkers.CIRCLE);
    new SwingWrapper<>(chart).displayChart();
    try {
      Thread.sleep(500_000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}