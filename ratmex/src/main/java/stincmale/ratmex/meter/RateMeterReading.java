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
import stincmale.ratmex.doc.NotThreadSafe;

/**
 * A modifiable {@link RateMeter} reading.
 */
@NotThreadSafe
public final class RateMeterReading {
  private long startNanos;
  private long tNanos;
  private long valueLong;
  private double valueDouble;
  private Duration unit;
  private boolean accurate;
  private boolean rounded;

  /**
   * All methods exposing the state of the newly constructed {@link RateMeterReading} return default values for variables of the type returned,
   * except for method {@link #getUnit()} which returns {@link Duration#ZERO}.
   */
  public RateMeterReading() {
  }

  /**
   * @param startNanos See {@link RateMeter#getStartNanos()}.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setStartNanos(final long startNanos) {
    this.startNanos = startNanos;
    return this;
  }

  /**
   * @return See {@link RateMeter#getStartNanos()}.
   */
  public final long getStartNanos() {
    return startNanos;
  }

  /**
   * @param safeTNanos See {@link #getTNanos()}.
   * This method does not check if {@code safeTNanos} is valid regarding the {@link #getStartNanos()}.
   * Implementations of {@link RateMeter} which fill {@link RateMeterReading} must guarantee that this value is valid.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setTNanos(final long safeTNanos) {
    this.tNanos = safeTNanos;
    return this;
  }

  /**
   * @return An instant (a pair (startNanos, elapsedNanos), see {@link RateMeter}) at which the reading was taken.
   * The more specific semantics may depend on the semantics of the {@link RateMeter}'s method via which the reading was made
   * and/or on the implementation details.
   */
  public final long getTNanos() {
    return tNanos;
  }

  /**
   * Sets both {@linkplain #getValueLong() long} and {@linkplain #getValueDouble() double} values.
   * Marks the reading as not {@linkplain #isRounded() rounded}.
   *
   * @param value A long value.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setValue(final long value) {
    valueLong = value;
    valueDouble = value;
    rounded = false;
    return this;
  }

  /**
   * Sets both {@linkplain #getValueDouble() double} and {@linkplain #getValueLong() long} values.
   * Marks the reading as {@linkplain #isRounded() rounded}.
   *
   * @param value A long value.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setValue(final double value) {
    valueLong = Math.round(value);
    valueDouble = value;
    rounded = true;
    return this;
  }

  /**
   * @return long representation of the value read. Either {@linkplain #isRounded() rounded}, or not.
   * Note that even if the reading is not {@linkplain #isRounded() rounded}, this value may not be equal to {@link #getValueDouble()}
   * because not every long value can be exactly represented as double value.
   */
  public final long getValueLong() {
    return valueLong;
  }

  /**
   * @return double representation of the value read.
   * Note that even if the reading is not {@linkplain #isRounded() rounded}, this value may not be equal to {@link #getValueLong()}
   * because not every long value can be exactly represented as double value.
   */
  public final double getValueDouble() {
    return valueDouble;
  }

  /**
   * @return A flag that specifies whether {@link #getValueLong()} returns a {@linkplain Math#round(double) rounded} representation of
   * {@link #getValueDouble()}, or not.
   */
  public final boolean isRounded() {
    return rounded;
  }

  /**
   * @param accurate See {@link #isAccurate()}.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setAccurate(final boolean accurate) {
    this.accurate = accurate;
    return this;
  }

  /**
   * @return A flag that specifies whether a {@link RateMeter} was able to provide an accurate reading,
   * or returned an average or an approximate rate (see {@link RateMeter#rate(long)}).
   */
  public final boolean isAccurate() {
    return accurate;
  }

  /**
   * @param safeUnit See {@link #getUnit()}.
   * This method does not check if {@code safeUnit} is valid.
   * Implementations of {@link RateMeter} which fill {@link RateMeterReading} must guarantee that this value is valid.
   *
   * @return {@code this}
   */
  public RateMeterReading setUnit(final Duration safeUnit) {
    this.unit = safeUnit;
    return this;
  }

  /**
   * @return A time interval in which value is measured, i.e. rate is measured in unit<sup>-1</sup>.
   */
  public final Duration getUnit() {
    return unit;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{startNanos=" + startNanos +
        ", tNanos=" + tNanos +
        ", valueLong=" + valueLong +
        ", valueDouble=" + valueDouble +
        ", unit=" + unit +
        ", accurate=" + accurate +
        ", rounded=" + rounded +
        '}';
  }
}