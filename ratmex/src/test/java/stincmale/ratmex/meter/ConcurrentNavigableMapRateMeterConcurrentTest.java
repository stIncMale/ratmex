package stincmale.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stincmale.ratmex.ConcurrencyTestTag;
import stincmale.ratmex.meter.ConcurrentRateMeterConfig.Builder;

@Tag(ConcurrencyTestTag.VALUE)
public final class ConcurrentNavigableMapRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentNavigableMapRateMeterConcurrentTest() {
    super(
        () -> (Builder)ConcurrentNavigableMapRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentNavigableMapRateMeter::new,
        Math.max(2, Runtime.getRuntime()
            .availableProcessors()));
  }
}