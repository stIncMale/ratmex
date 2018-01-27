package stincmale.ratmex.meter;

import java.util.concurrent.atomic.LongAdder;
import stincmale.ratmex.doc.ThreadSafe;

/**
 * This {@link TicksCounter} uses {@link LongAdder} field to store its {@linkplain #get() value}.
 */
@ThreadSafe
public final class LongAdderTicksCounter extends AbstractTicksCounter {
  private final LongAdder adder;

  public LongAdderTicksCounter(final long initialValue) {
    adder = new LongAdder();
    adder.add(initialValue);
  }

  @Override
  public final void add(final long delta) {
    adder.add(delta);
  }

  @Override
  public final long get() {
    return adder.sum();
  }
}