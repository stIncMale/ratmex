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
import java.util.function.Function;
import java.util.function.Supplier;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.Immutable;
import stincmale.ratmex.doc.NotThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration that can be used to create concurrent implementations of {@link AbstractRateMeter},
 * e.g. {@link ConcurrentNavigableMapRateMeter}, {@link ConcurrentRingBufferRateMeter}.
 * <p>
 * The default values:
 * <ul>
 * <li>The default values from {@link RateMeterConfig} except for {@link #getTicksCounterSupplier()}, {@link #getHistoryLength()}</li>
 * <li>{@link #getTicksCounterSupplier()} - {@link LongAdderTicksCounter LongAdderTicksCounter::new}</li>
 * <li>{@link #getHistoryLength()} - 64</li>
 * <li>{@link #getMaxTicksCountAttempts()} - 16</li>
 * <li>{@link #getMode()} - {@link Mode#STRICT}</li>
 * <li>{@link #isCollectStats()} - false</li>
 * <li>{@link #getWaitStrategySupplier()} - {@link YieldWaitStrategy#instance() YieldWaitStrategy::instance}</li>
 * <li>{@link #getLockStrategySupplier()} -
 * {@link SpinLockStrategy new SpinLockStrategy(}{@link YieldWaitStrategy YieldWaitStrategy.instance())}</li>
 * </ul>
 */
@Immutable
public class ConcurrentRateMeterConfig extends RateMeterConfig {
  private final int maxTicksCountAttempts;
  private final Mode mode;
  private final boolean collectStats;
  private final Supplier<? extends WaitStrategy> waitStrategySupplier;
  private final Supplier<? extends LockStrategy> lockStrategySupplier;

  /**
   * @param ticksCounterSupplier See {@link RateMeterConfig#RateMeterConfig(Function, Duration, int)}.
   * @param timeSensitivity See {@link RateMeterConfig#RateMeterConfig(Function, Duration, int)}.
   * @param historyLength See {@link RateMeterConfig#RateMeterConfig(Function, Duration, int)}.
   * @param maxTicksCountAttempts See {@link Builder#setMaxTicksCountAttempts(int)}.
   * @param mode See {@link Builder#setMode(Mode)}.
   * @param collectStats See {@link Builder#setCollectStats(boolean)}.
   * @param waitStrategySupplier See {@link Builder#setWaitStrategySupplier(Supplier)}.
   * @param lockStrategySupplier See {@link Builder#setLockStrategySupplier(Supplier)}.
   */
  protected ConcurrentRateMeterConfig(
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      @Nullable final Duration timeSensitivity,
      final int historyLength,
      final int maxTicksCountAttempts,
      final Mode mode,
      final boolean collectStats,
      final Supplier<? extends WaitStrategy> waitStrategySupplier,
      final Supplier<? extends LockStrategy> lockStrategySupplier) {
    super(
        ticksCounterSupplier,
        timeSensitivity,
        historyLength);
    checkArgument(maxTicksCountAttempts > 0, "maxTicksCountAttempts", "Must be positive");
    checkNotNull(waitStrategySupplier, "waitStrategySupplier");
    checkNotNull(lockStrategySupplier, "lockStrategySupplier");
    this.maxTicksCountAttempts = maxTicksCountAttempts;
    this.mode = mode;
    this.collectStats = collectStats;
    this.waitStrategySupplier = waitStrategySupplier;
    this.lockStrategySupplier = lockStrategySupplier;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public Builder toBuilder() {
    return new Builder().set(this);
  }

  /**
   * Specifies the desired maximum number of attempts to calculate the number of ticks (see {@link RateMeter#rate()} for example).
   * Note that this is just a hint, so an implementation may choose to do more attempts, but the number of attempts must be finite.
   * <p>
   * <b>The reasoning behind this hint</b><br>
   * Implementations may allow a race condition (for performance reasons) while counting ticks.
   * When running out of the number of attempts such implementations may choose to fall over to an approach that excludes the race
   * and allows to eventually count the ticks.
   */
  public final int getMaxTicksCountAttempts() {
    return maxTicksCountAttempts;
  }

  /**
   * Specifies a {@linkplain Mode mode} of a {@link RateMeter}.
   *
   * @see ConcurrentRateMeterStats#incorrectlyRegisteredTickEventsCount()
   */
  public final Mode getMode() {
    return mode;
  }

  /**
   * This configuration parameter specifies if a {@link RateMeter} which can collect {@link RateMeter#stats() stats} must do so.
   */
  public final boolean isCollectStats() {
    return collectStats;
  }

  /**
   * Specifies which {@link WaitStrategy} must be used by a thread-safe implementation {@link RateMeter} (if it at all uses it).
   */
  public final Supplier<? extends WaitStrategy> getWaitStrategySupplier() {
    return waitStrategySupplier;
  }

  /**
   * Specifies which {@link LockStrategy} must be used by a thread-safe implementation {@link RateMeter} (if it at all uses it).
   */
  public final Supplier<? extends LockStrategy> getLockStrategySupplier() {
    return lockStrategySupplier;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{ticksCounterSupplier=" + getTicksCounterSupplier() +
        ", timeSensitivity=" + getTimeSensitivity().orElse(null) +
        ", historyLength=" + getHistoryLength() +
        ", maxTicksCountAttempts=" + maxTicksCountAttempts +
        ", mode=" + mode +
        ", collectStats=" + collectStats +
        ", waitStrategySupplier=" + waitStrategySupplier +
        ", lockStrategySupplier=" + lockStrategySupplier +
        '}';
  }

  @NotThreadSafe
  public static class Builder extends RateMeterConfig.Builder {
    protected int maxTicksCountAttempts;
    protected Mode mode;
    protected boolean collectStats;
    protected Supplier<? extends WaitStrategy> waitStrategySupplier;
    protected Supplier<? extends LockStrategy> lockStrategySupplier;

    protected Builder() {
      ticksCounterSupplier = LongAdderTicksCounter::new;
      historyLength = 64;
      maxTicksCountAttempts = 16;
      mode = Mode.STRICT;
      collectStats = false;
      waitStrategySupplier = YieldWaitStrategy::instance;
      lockStrategySupplier = () -> new SpinLockStrategy(YieldWaitStrategy.instance());
    }

    /**
     * @param config Must not be null.
     */
    public final Builder set(final ConcurrentRateMeterConfig config) {
      checkNotNull(config, "config");
      set((RateMeterConfig)config);
      maxTicksCountAttempts = config.getMaxTicksCountAttempts();
      mode = config.getMode();
      collectStats = config.isCollectStats();
      waitStrategySupplier = config.getWaitStrategySupplier();
      lockStrategySupplier = config.getLockStrategySupplier();
      return this;
    }

    /**
     * @param maxTicksCountAttempts Must be positive.
     *
     * @see ConcurrentRateMeterConfig#getMaxTicksCountAttempts()
     */
    public final RateMeterConfig.Builder setMaxTicksCountAttempts(final int maxTicksCountAttempts) {
      checkArgument(maxTicksCountAttempts > 0, "maxTicksCountAttempts", "Must be positive");
      this.maxTicksCountAttempts = maxTicksCountAttempts;
      return this;
    }

    /**
     * @param mode Must not be null.
     *
     * @see ConcurrentRateMeterConfig#getMode()
     */
    public final Builder setMode(final Mode mode) {
      this.mode = mode;
      return this;
    }

    /**
     * @see ConcurrentRateMeterConfig#isCollectStats()
     */
    public final Builder setCollectStats(final boolean collectStats) {
      this.collectStats = collectStats;
      return this;
    }

    /**
     * @param waitStrategySupplier Must not be null.
     *
     * @see ConcurrentRateMeterConfig#getWaitStrategySupplier()
     */
    public final Builder setWaitStrategySupplier(final Supplier<? extends WaitStrategy> waitStrategySupplier) {
      checkNotNull(waitStrategySupplier, "waitStrategySupplier");
      this.waitStrategySupplier = waitStrategySupplier;
      return this;
    }

    /**
     * @param lockStrategySupplier Must not be null.
     *
     * @see ConcurrentRateMeterConfig#getLockStrategySupplier()
     */
    public final Builder setLockStrategySupplier(final Supplier<? extends LockStrategy> lockStrategySupplier) {
      checkNotNull(lockStrategySupplier, "lockStrategySupplier");
      this.lockStrategySupplier = lockStrategySupplier;
      return this;
    }

    @Override
    public ConcurrentRateMeterConfig build() {
      return new ConcurrentRateMeterConfig(
          ticksCounterSupplier,
          timeSensitivity,
          historyLength,
          maxTicksCountAttempts,
          mode,
          collectStats,
          waitStrategySupplier,
          lockStrategySupplier);
    }
  }

  /**
   * A mode of {@link RateMeter}.
   * Implementations must specify supported modes and should explain semantics.
   */
  public enum Mode {
    /**
     * In this mode {@link RateMeter} is allowed to {@linkplain RateMeter#tick(long, long) register} ticks incorrectly
     * (i.e. either loose some ticks, or register them at the incorrect instant).
     * Such a relaxation may allow an implementation to display drastically better performance.
     * <p>
     * Implementations should provide information about such incorrect registrations on the best effort basis
     * (e.g. via {@linkplain ConcurrentRateMeterStats#incorrectlyRegisteredTickEventsCount()}).
     */
    RELAXED_TICKS,
    /**
     * A default mode.
     * In this mode {@link RateMeter} is guaranteed to correctly {@linkplain RateMeter#tick(long, long) register} all ticks.
     */
    STRICT
  }
}