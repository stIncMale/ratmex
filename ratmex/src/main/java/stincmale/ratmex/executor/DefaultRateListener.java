package stincmale.ratmex.executor;

import stincmale.ratmex.doc.NotThreadSafe;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateException} if notices that the target rate is not respected.
 *
 * @param <E> A type of a {@link RateMeasuredEvent} which this listener can react to.
 */
@NotThreadSafe
public class DefaultRateListener<E extends RateMeasuredEvent> implements RateListener<E> {
  private static final DefaultRateListener<RateMeasuredEvent> instance = new DefaultRateListener<>();

  protected DefaultRateListener() {
  }

  /**
   * @param <E> See {@link DefaultRateListener}.
   *
   * @return A default {@link RateListener}.
   */
  @SuppressWarnings("unchecked")
  public static <E extends RateMeasuredEvent> DefaultRateListener<E> defaultRateListenerInstance() {
    return (DefaultRateListener<E>)instance;
  }

  /**
   * @throws RateException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected.
   */
  @Override
  public boolean onMeasurement(final E e) throws RateException {
    if (e.getTargetRate()
        .compareTo(e.getCompletionRate()) != 0) {
      throw new RateException(
          "The completion rate violated the target rate. ",
          e.getTargetRate(),
          e.getCompletionRate()
              .getValueDouble());
    }
    return true;
  }
}