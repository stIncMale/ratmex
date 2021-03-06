/*
 * Copyright 2017-2018 Valiantsin Kavalenka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stincmale.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.checkTNanos;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.checkUnit;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.convertRate;
import static stincmale.ratmex.internal.util.ConversionsAndChecks.maxTNanos;

/**
 * A utility that measures a rate of ticks by counting ticks in a moving time interval.
 * <p>
 * <b>Glossary</b><br>
 * <i>Tick</i><br>
 * A tick is any event which is registered via {@link #tick(long, long)} method.
 * <p>
 * <i>Instant</i><br>
 * {@link RateMeter} treats instants as the time (number of nanoseconds) elapsed since the {@linkplain #getStartNanos() start}.
 * So an instant is a pair (startNanos, elapsedNanos), but because startNanos is known and fixed,
 * we can equivalently specify an instant via a single value tNanos = startNanos + elapsedNanos (note that tNanos >= startNanos).
 * {@link RateMeter} uses tNanos notation instead of (startNanos, elapsedNanos) notation.
 * All nanosecond values are compared as specified by {@link System#nanoTime()}.
 * <p>
 * <i>Sample</i><br>
 * A sample is a pair (tNanos, ticksCount).
 * <p>
 * <i>Samples window</i><br>
 * A samples window is a half-closed time interval
 * ({@linkplain #rightSamplesWindowBoundary() rightmostRegisteredInstant} - {@linkplain #getSamplesInterval() samplesInterval};
 * {@linkplain #rightSamplesWindowBoundary() rightmostRegisteredInstant}]
 * (comparison according to {@link System#nanoTime()}).
 * Samples window can only be moved to the right and the only way to do this is to call {@link #tick(long, long)}.
 * <pre>{@code
 *                                        (-------movedSamplesWindow----]
 *                                        |                             |
 *   (--------samplesWindow--------]      |<------samplesInterval------>|                                              t
 * --|-----------------------------|------|-----------------------------|--------------------------------------------->
 *                                 |                                    |
 *                             startNanos                             tNanos
 *                                 |<-----------elapsedNanos----------->|
 * }</pre>
 * <p>
 * <i>Current ticks</i><br>
 * Current ticks are those registered inside the current samples window.
 * <p>
 * <i>Rate</i><br>
 * A rate (a.k.a. instant rate, current rate) is calculated based on current ticks
 * and by default is measured in samplesInterval<sup>-1</sup>, hence a current rate value always equals to a current ticks count.
 * <p>
 * For example if samplesInterval is 30ns,<br>
 * startNanos is 10ns,<br>
 * and the only registered ticks are<br>
 * (25ns, 1) (this is tNanos, not tNanos - startNanos),<br>
 * (30ns, 1),<br>
 * (50ns, 8),<br>
 * (60ns, -2),<br>
 * then the current rate is<br>
 * (8 - 2) / samplesInterval = 6samplesInterval<sup>-1</sup> = 6 / 30ns = 0.2ns<sup>-1</sup>.
 * <pre>{@code
 *       10ns           25ns 30ns                50ns      60ns                                                        t
 * --|----|----|----|----1----1----|----|----|----8----|---(-2)--|----|----|----|----|----|----|----|----|----|----|-->
 *        |                   (--------samplesWindow--------]
 *    startNanos
 * }</pre>
 * <p>
 * <i>Samples history</i><br>
 * A half-closed interval which is equal to the union of adjacent samples windows. Samples history includes samples registered inside it.
 * The length of a samples history is always a multiple of the length of a samples window (i.e. {@linkplain #getSamplesInterval() samplesInterval}).
 * <pre>{@code
 *                  (-----------------------------*--------samplesWindow--------]
 *                                                |                             |
 *                             leftNanos is not included in samplesWindow       |
 *                                 but is included in samplesHistory            |
 *                                                |                             |                                      t
 * -----------------|-----------------------------|-----------------------------|------------------------------------->
 *                  |                         leftNanos                         |
 *                  |                                                           |
 *                  |<-------------samplesHistory, historyLength==2------------>|
 * }</pre>
 * <p>
 * <i>Safe samples history</i><br>
 * A part of samples history that includes everything except for the leftmost samples window.
 * It is possible to calculate the rate for any instant inside the safe samples history, or ahead (to the right) of it.
 * The minimal allowed length of samples history is 2samplesInterval
 * because this gives the minimal possible safe samples history with length equal to samplesInterval.
 * <pre>{@code
 *                  (-----------------------------*-----------------------------*--------samplesWindow--------]        t
 * -----------------|-----------------------------|------------------------------------------------------------------->
 *                  |                             |                                                           |
 *                  |                             |<--------------------safeSamplesHistory------------------->|
 *                  |                                                                                         |
 *                  |<------------------------- safeSamplesHistory, historyLength==3------------------------->|
 * }</pre>
 * <p>
 * <b>Allowed values</b><br>
 * ticksCount \u2208 [{@link Long#MIN_VALUE}; {@link Long#MAX_VALUE}],<br>
 * samplesInterval (in nanos) \u2208 [1, {@link Long#MAX_VALUE} / (historyLength + 1) - 1],<br>
 * tNanos \u2208 [startNanos, startNanos - (historyLength + 1) * samplesInterval + {@link Long#MAX_VALUE}]
 * (comparison according to {@link System#nanoTime()}).
 * <p>
 * <b>API notes</b><br>
 * This interface is designed to allow garbage-free implementations and usage scenarios.
 * Methods with {@link RateMeterReading} parameter are specifically serve this purpose:
 * they do not create {@link RateMeterReading} objects, thus allowing a user to reuse the same {@link RateMeterReading}.
 * <p>
 * <b>Implementation notes</b><br>
 * Different implementations may conform to different correctness conditions (a.k.a. consistency models)
 * and provide different guarantees on the accuracy.
 * Implementations with weaker guarantees may display better performance because they
 * can sacrifice accuracy for the sake of performance and yet may produce sufficiently accurate results in practice.
 * Implementations are recommended to aim for accuracy on the best-effort basis,
 * but all methods which have analogs with {@link RateMeterReading} parameters
 * are allowed to produce {@link RateMeterReading#isAccurate() approximate} results.
 * <p>
 * Implementations may not internally use nanosecond {@linkplain #getTimeSensitivity time sensitivity} (resolution, accuracy, granularity).
 * In fact, there is no sense in using resolution better than the resolution of the timer that is used by a user of {@link RateMeter}.
 *
 * @param <S> A type that represents {@linkplain #stats() statistics}.
 */
public interface RateMeter<S> {
  /**
   * Returns the starting point that is used to calculate elapsed nanoseconds.
   *
   * @return The starting point that is used to calculate elapsed nanoseconds.
   *
   * @see System#nanoTime()
   */
  long getStartNanos();

  /**
   * A size of the samples window.
   *
   * @return {@link Duration} which is not {@linkplain Duration#isZero() zero}
   * and not {@linkplain Duration#isNegative() negative}.
   */
  Duration getSamplesInterval();

  /**
   * A time sensitivity which affects the behaviour of {@link RateMeter#tick(long, long)} method in a way that allows registering a sample
   * at an instant that differs from the specified one not more than by the time sensitivity.
   * <p>
   * This behavior may be observed via {@link #rightSamplesWindowBoundary()}.
   * Hence this method can be considered as an important implementation detail leaked through {@link RateMeter}'s
   * <a href="https://www.joelonsoftware.com/2002/11/11/the-law-of-leaky-abstractions/">leaky abstraction</a>.
   * <p>
   * <b>Implementation considerations</b><br>
   * It is recommended to use a resolution (accuracy, granularity) of the timer used by a user of {@link RateMeter}.
   * For example 200ns may be a good approximation of the {@link System#nanoTime()} accuracy
   * (see <a href="https://github.com/shipilev/timers-bench">timers-bench</a>
   * and <a href="https://shipilev.net/blog/2014/nanotrusting-nanotime/">Nanotrusting the Nanotime</a> for measurements and explanations).
   *
   * @return A positive, i.e. not {@linkplain Duration#isNegative() negative} and not {@linkplain Duration#isZero() zero} time sensitivity which
   * is used internally.
   */
  Duration getTimeSensitivity();

  /**
   * Instant that corresponds to the right border of the samples window.
   * At the very beginning this is equal to {@link #getStartNanos()}.
   * This border can be moved to the right via {@link #tick(long, long)} method.
   *
   * @return The rightmost {@linkplain #tick(long, long) registered} instant.
   */
  long rightSamplesWindowBoundary();

  /**
   * Calculates the total number of ticks since the {@linkplain #getStartNanos() start}.
   *
   * @return Total number of ticks.
   */
  long ticksCountTotal();

  /**
   * Registers a sample of {@code count} ticks at {@code tNanos} instant.
   * If {@code tNanos} is greater than current {@link #rightSamplesWindowBoundary()}
   * then this method moves the samples window such that its right boundary is at {@code tNanos}.
   *
   * @param count Number of ticks. May be negative, zero, or positive.
   * If zero then the method does nothing,
   * otherwise adds {@code count} to the currently registered number of ticks at the specified instant,
   * or just remembers {@code count} ticks if no ticks were registered at the specified instant.
   * @param tNanos An instant (a pair (startNanos, elapsedNanos)) at which {@code count} ticks need to be registered.
   */
  void tick(final long count, final long tNanos);

  /**
   * Calculates an average (mean) rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start}
   * till the {@link #rightSamplesWindowBoundary()}.
   *
   * @return The same value as {@link #rateAverage(long) rateAverage}{@code (}{@link #rightSamplesWindowBoundary()}{@code )}.
   */
  default double rateAverage() {//TODO override and make final?
    return rateAverage(rightSamplesWindowBoundary());
  }

  /**
   * This method is equivalent to {@link #rateAverage()}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rateAverage(final Duration unit) {
    checkUnit(unit, "unit");
    return convertRate(rateAverage(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates an average (mean) rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start} till the {@code tNanos},
   * if {@code tNanos} is greater than {@link #rightSamplesWindowBoundary()},
   * otherwise returns {@link #rateAverage()}.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   *
   * @return Average rate of ticks or 0 if {@code tNanos} is equal to {@link #getStartNanos()}.
   */
  double rateAverage(final long tNanos);

  /**
   * This method is equivalent to {@link #rateAverage(long)}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  double rateAverage(final long tNanos, final Duration unit);

  /**
   * Calculates the current rate of ticks measured in {@linkplain #getSamplesInterval() samplesInterval}<sup>-1</sup>.
   * Conceptually, the returned value is equal to {@link #rate(long) rate}{@code (}{@link #rightSamplesWindowBoundary()}{@code )},
   * but with both methods {@link #rightSamplesWindowBoundary()} and {@link #rate(long)} executed atomically.
   *
   * @return The current rate of ticks.
   */
  long rate();

  /**
   * This method is equivalent to {@link #rate()}, but provides a complete measured data,
   * while {@link #rate()} returns only a rate value.
   *
   * @param reading A {@link RateMeterReading} to be filled with the measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  RateMeterReading rate(final RateMeterReading reading);

  /**
   * This method is equivalent to {@link #rate()}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rate(final Duration unit) {
    checkUnit(unit, "unit");
    return convertRate(rate(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * This method is equivalent to {@link #rate(Duration)}, but provides a complete measured data,
   * while {@link #rate(Duration)} returns only a rate value.
   *
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @param reading A {@link RateMeterReading} to be filled with the measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  default RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    checkUnit(unit, "unit");
    checkNotNull(reading, "reading");
    return convertRate(rate(reading), unit);
  }

  /**
   * Calculates rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * as if {@code tNanos} were the right boundary of the samples window,
   * if {@code tNanos} is ahead of or within the safe samples history;
   * otherwise returns {@link #rateAverage()}.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   */
  default double rate(long tNanos) {
    return rate(tNanos, new RateMeterReading()).getValueDouble();
  }

  /**
   * This method is equivalent to {@link #rate(long)}, but provides a complete measured data,
   * while {@link #rate(long)} returns only a rate value.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param reading A {@link RateMeterReading} to be filled with the measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  RateMeterReading rate(long tNanos, RateMeterReading reading);

  /**
   * This method is equivalent to {@link #rate(long)}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  double rate(final long tNanos, final Duration unit);

  /**
   * This method is equivalent to {@link #rate(long, Duration)}, but provides a complete measured data,
   * while {@link #rate(long, Duration)} returns only a rate value.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @param reading A {@link RateMeterReading} to be filled with the measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  default RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    final long startNanos = getStartNanos();
    final Duration samplesInterval = getSamplesInterval();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos, 3), "tNanos");
    checkUnit(unit, "unit");
    checkNotNull(reading, "reading");
    return convertRate(rate(tNanos, reading), unit);
  }

  /**
   * Returns statistics gathered by this {@link RateMeter}.
   *
   * @return Stats which may be not {@linkplain Optional#isPresent() present}
   * if the {@link RateMeter} does not collect stats. Once returned a non-empty {@link Optional}, this method must always return
   * an {@link Optional} containing the same object, so that users can store a reference to the object and reuse it.
   */
  Optional<? extends S> stats();
}