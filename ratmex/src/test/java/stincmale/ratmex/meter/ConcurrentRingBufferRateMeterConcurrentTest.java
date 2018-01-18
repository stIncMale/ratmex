package stincmale.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stincmale.ratmex.util.ConcurrencyTestTag;
import stincmale.ratmex.meter.ConcurrentRateMeterConfig.Builder;

@Tag(ConcurrencyTestTag.VALUE)
public final class ConcurrentRingBufferRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentRingBufferRateMeterConcurrentTest() {
    super(
        () -> (Builder)ConcurrentRingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentRingBufferRateMeter::new,
        Math.max(2, Runtime.getRuntime()
            .availableProcessors()));
  }
}