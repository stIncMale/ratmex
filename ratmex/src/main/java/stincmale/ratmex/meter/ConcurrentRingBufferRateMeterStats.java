package stincmale.ratmex.meter;

import java.util.concurrent.atomic.LongAdder;
import stincmale.ratmex.doc.ThreadSafe;

@ThreadSafe
final class ConcurrentRingBufferRateMeterStats implements ConcurrentRateMeterStats {
  private final LongAdder incorrectlyRegisteredTickEventsCounter;

  ConcurrentRingBufferRateMeterStats() {
    incorrectlyRegisteredTickEventsCounter = new LongAdder();
  }

  @Override
  public final long incorrectlyRegisteredTickEventsCount() {
    return incorrectlyRegisteredTickEventsCounter.sum();
  }

  public final void registerIncorrectlyRegisteredTicksEvent() {
    incorrectlyRegisteredTickEventsCounter.increment();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{incorrectlyRegisteredTickEventsCounter=" + incorrectlyRegisteredTickEventsCounter +
        '}';
  }
}