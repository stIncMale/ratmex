package stincmale.ratmex.performance;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import stincmale.ratmex.performance.meter.RateMeterPerformanceTest;
import stincmale.ratmex.performance.util.PerformanceTestResult;

@TestInstance(Lifecycle.PER_METHOD)
public final class ChartsGenerator {
  public ChartsGenerator() {
  }

  @Test
  public final void run() {
    final Collection<PerformanceTestResult> loadedPtrs = PerformanceTestResult.loadAll(ChartsGenerator.class);
    RateMeterPerformanceTest.generateCharts(loadedPtrs);
    BaselinePerformanceTest.generateCharts(loadedPtrs);
  }
}