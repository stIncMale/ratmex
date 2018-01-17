package stincmale.ratmex.meter;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;
import stincmale.ratmex.internal.util.Preconditions;

/**
 * This implementation of {@link WaitStrategy} just spins calling {@link Thread#yield()}
 * between tests of a {@linkplain #await(BooleanSupplier) condition}.
 */
@ThreadSafe
public final class YieldWaitStrategy implements WaitStrategy {
  private static final YieldWaitStrategy instance = new YieldWaitStrategy();

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link YieldWaitStrategy}.
   */
  public static final YieldWaitStrategy instance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    Preconditions.checkNotNull(condition, "condition");
    while (!condition.getAsBoolean()) {
      Thread.onSpinWait();
      Thread.yield();
    }
  }

  private YieldWaitStrategy() {
  }
}