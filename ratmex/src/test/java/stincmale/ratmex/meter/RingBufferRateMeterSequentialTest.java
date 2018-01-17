package stincmale.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stincmale.ratmex.ConcurrencyTestTag;
import stincmale.ratmex.meter.RateMeterConfig.Builder;

@Tag(ConcurrencyTestTag.VALUE)
public final class RingBufferRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterSequentialTest() {
    super(
        () -> RingBufferRateMeter.defaultConfig()
            .toBuilder(),
        RingBufferRateMeter::new, 1);
  }
}