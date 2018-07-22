package stincmale.ratmex.executor.listener;

import stincmale.ratmex.doc.NotThreadSafe;
import stincmale.ratmex.executor.Rate;
import stincmale.ratmex.executor.RateMeasuringExecutorService;
import stincmale.ratmex.executor.config.ScheduledTaskConfig;

/**
 * A container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 * This container may be mutable, and must always be treated as such.
 */
@NotThreadSafe
public interface RateMeasuredEvent {
  /**
   * @return The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig)}.
   */
  Rate getTargetRate();
}