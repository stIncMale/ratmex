package stincmale.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stincmale.ratmex.TestTag;
import stincmale.ratmex.meter.RateMeterConfig.Builder;

@Tag(TestTag.CONCURRENCY)
public final class NavigableMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterSequentialTest() {
    super(() -> NavigableMapRateMeter.defaultConfig()
            .toBuilder(),
        NavigableMapRateMeter::new, 1);
  }
}