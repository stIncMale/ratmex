package stincmale.ratmex.executor.auxiliary;

import stincmale.ratmex.doc.ThreadSafe;

/**
 * An implementation of {@link Time} that represents actual physical time, e.g. {@link System#nanoTime()}.
 */
@ThreadSafe
public final class SystemTime implements Time {
  private static final SystemTime instance = new SystemTime();

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link SystemTime}.
   */
  public static final SystemTime instance() {
    return instance;
  }

  private SystemTime() {
  }
}