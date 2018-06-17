package stincmale.ratmex.executor;

import stincmale.ratmex.doc.Nullable;

/**
 * See {@link RateListener#onMeasurement(RateMeasuredEvent)}.
 */
public class CorrectnessException extends RuntimeException {
  private static final long serialVersionUID = -5116482860213079312L;

  public CorrectnessException(@Nullable final String message) {
    super(message);
  }
}