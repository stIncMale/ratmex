package stincmale.ratmex.executor;

import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.meter.RateMeterReading;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 */
@NotThreadSafe
public class RateMeasuredEvent {
  private final Rate targetRate;
  private final RateMeterReading completionRate;

  /**
   * @param targetRate The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be {@code null}.
   * @param completionRate The current completion rate of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   * Must not be {@code null}.
   */
  public RateMeasuredEvent(final Rate targetRate, final RateMeterReading completionRate) {
    checkNotNull(targetRate, "targetRate");
    checkNotNull(completionRate, "completionRate");
    this.targetRate = targetRate;
    this.completionRate = completionRate;
  }

  /**
   * @return The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final Rate getTargetRate() {
    return targetRate;
  }

  /**
   * @return The current completion rate of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final RateMeterReading getCompletionRate() {
    return completionRate;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + targetRate +
        ", completionRate=" + completionRate +
        '}';
  }
}