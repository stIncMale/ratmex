package stincmale.ratmex.executor;

import stincmale.ratmex.doc.Nullable;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * See {@link RateListener#onMeasurement(RateMeasuredEvent)}.
 */
public class RateException extends RuntimeException {
  private static final long serialVersionUID = 8211810094629034496L;

  private final Rate targetRate;
  private final double actualRateValue;

  /**
   * @param clarification A text that clarifies this exception. The {@linkplain RuntimeException#getMessage() message} is constructed as
   * {@code (clarification == null ? "" : clarification) + "targetRate=%s, actualRateValue=%s"},
   * where {@code %s} placeholders are substituted with the arguments of this constructor.
   * @param targetRate See {@link #getTargetRate()}. Must not be {@code null}.
   * @param actualRateValue See {@link #getActualRateValue()}.
   */
  public RateException(@Nullable final String clarification, final Rate targetRate, final double actualRateValue) {
    super((clarification == null ? "" : clarification) +
        format("targetRate=%s, actualRateValue=%s", checkNotNull(targetRate, "targetRate"), actualRateValue));
    this.targetRate = targetRate;
    this.actualRateValue = actualRateValue;
  }

  /**
   * @return The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduledTaskConfig) scheduled task}.
   */
  public final Rate getTargetRate() {
    return targetRate;
  }

  /**
   * @return The actual rate value measured in {@link Rate#getUnit() units} specified by the {@code targetRate}.
   */
  public final double getActualRateValue() {
    return actualRateValue;
  }
}