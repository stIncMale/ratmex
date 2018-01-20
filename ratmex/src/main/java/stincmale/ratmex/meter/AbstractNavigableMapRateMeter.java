package stincmale.ratmex.meter;

import java.time.Duration;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import stincmale.ratmex.NanosComparator;
import stincmale.ratmex.internal.util.ConversionsAndChecks;
import stincmale.ratmex.internal.util.Preconditions;
import static stincmale.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;
import static stincmale.ratmex.internal.util.Utils.format;

/**
 * This is an almost complete implementation of {@link AbstractRateMeter}
 * which only requires a correct parameters to be provided to the
 * {@linkplain #AbstractNavigableMapRateMeter(long, Duration, ConcurrentRateMeterConfig, Supplier, boolean) constructor}.
 * Depending on the arguments the constructed object can be either sequential, or concurrent.
 * <p>
 * This implementation of {@link RateMeter} uses a {@link NavigableMap} to store and access a samples history.
 * <p>
 * <i>Advantages</i><br>
 * <ul>
 * <li>Unlike {@link AbstractRingBufferRateMeter}, this implementation tolerates a large ratio of
 * {@link #getSamplesInterval()} to {@link #getTimeSensitivity()}.
 * The reason for this is that it only creates objects representing samples when necessary,
 * hence potentially reduces the number of samples that must be added up to count the {@linkplain #ticksCount() current ticks}.</li>
 * </ul>
 * <p>
 * <i>Disadvantages</i><br>
 * <ul>
 * <li>Unlike {@link AbstractRingBufferRateMeter}, this implementation produces garbage.</li>
 * <li>Unlike {@link AbstractRingBufferRateMeter}, this implementation does not benefit from the idea of memory locality of data.</li>
 * </ul>
 *
 * @param <C> A type of the {@linkplain #getConfig() configuration}.
 */
public abstract class AbstractNavigableMapRateMeter<C extends ConcurrentRateMeterConfig> extends AbstractRateMeter<Void, C> {
  private final boolean sequential;
  private final NavigableMap<Long, TicksCounter> samplesHistory;
  private final int maxTicksCountAttempts;
  @Nullable
  private final AtomicBoolean atomicCleanInProgress;//we don't need an analogous field for a sequential implementation
  private volatile long volatileCleanLastRightSamplesWindowBoundary;//cleanLastRightSamplesWindowBoundary for a concurrent implementation
  private long cleanLastRightSamplesWindowBoundary;//for a sequential implementation
  @Nullable
  private final LockStrategy ticksCountLock;//we don't need an analogous field for a sequential implementation

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   * @param samplesHistorySupplier A supplier providing an object that will be used to store and access the samples history.
   * The {@link NavigableMap} provided by this supplier must use {@link NanosComparator} as {@link NavigableMap#comparator() comparator},
   * and must be thread-safe if {@code sequential} argument is false. If these constraints are violated, then the behavior is unspecified.
   * @param sequential Specifies whether the {@link RateMeter} must be thread-safe (will be used concurrently, so the value is false),
   * or not (will be used sequentially, so the value is true).
   */
  protected AbstractNavigableMapRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final C config,
      final Supplier<? extends NavigableMap<Long, TicksCounter>> samplesHistorySupplier,
      final boolean sequential) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesHistorySupplier, "samplesHistorySupplier");
    samplesHistory = samplesHistorySupplier.get();
    Preconditions.checkArgument(samplesHistory.comparator() instanceof NanosComparator, "samplesSupplier",
        () -> "The comparator used by samples history map must be of type " + NanosComparator.class.getSimpleName());
    samplesHistory.put(
        startNanos,
        config.getTicksCounterSupplier()
            .apply(0L));
    atomicCleanInProgress = sequential ? null : new AtomicBoolean();
    volatileCleanLastRightSamplesWindowBoundary = getStartNanos();
    cleanLastRightSamplesWindowBoundary = getStartNanos();
    Preconditions.checkArgument(getTimeSensitivityNanos() <= getSamplesIntervalNanos(), "config",
        () -> format("timeSensitivity=%sns must be not greater than samplesInterval=%sns",
            getTimeSensitivityNanos(), getSamplesIntervalNanos()));
    ticksCountLock = sequential
        ? null
        : config.getLockStrategySupplier()
            .get();
    this.sequential = sequential;
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    return samplesHistory.lastKey();
  }

  /*The implementation of this method is an exact copy of ticksCount(RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final long ticksCount() {
    final long value;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      value = count(leftNanos, rightNanos);
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          final long leftNanos = rightNanos - samplesIntervalNanos;
          final long count = count(leftNanos, rightNanos);
          final long newRightNanos = rightSamplesWindowBoundary();
          final long newLeftmostHistoryNanos = newRightNanos - getConfig().getHistoryLength() * samplesIntervalNanos;
          if (NanosComparator.compare(newLeftmostHistoryNanos, leftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but value is still correct
            value = count;
            break;
          } else {//the samples window has been moved too far
            rightNanos = newRightNanos;
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples window too far.
              However since tick method acquires the write lock not always, but only if sees the read lock acquired,
              there is a race condition which still may lead to the samples window being moved,
              though the likelihood of such situation is now much less.*/
            if (ticksCountReadLockStamp == 0 && readIteration >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              if (trySharedLockAttempts > 0) {
                trySharedLockAttempts--;
                ticksCountReadLockStamp = ticksCountLock.trySharedLock();
              } else {
                ticksCountReadLockStamp = ticksCountLock.sharedLock();
              }
            }
          }
          readIteration = Math.addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountReadLockStamp);
        }
      }
    }
    return value;
  }

  /**
   * @return {@inheritDoc}
   * The reading is always {@linkplain RateMeterReading#isAccurate() accurate}.
   */
  @Override
  public final RateMeterReading ticksCount(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    reading.setAccurate(true);
    final boolean readingDone;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      reading.setValue(count(leftNanos, rightNanos));
      readingDone = true;
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          final long leftNanos = rightNanos - samplesIntervalNanos;
          final long count = count(leftNanos, rightNanos);
          final long newRightNanos = rightSamplesWindowBoundary();
          final long newLeftmostHistoryNanos = newRightNanos - getConfig().getHistoryLength() * samplesIntervalNanos;
          if (NanosComparator.compare(newLeftmostHistoryNanos, leftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but value is still correct
            reading.setValue(count);
            readingDone = true;
            break;
          } else {//the samples window has been moved too far
            rightNanos = newRightNanos;
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples window too far.
              However since tick method acquires the write lock not always, but only if sees the read lock acquired,
              there is a race condition which still may lead to the samples window being moved,
              though the likelihood of such situation is now much less.*/
            if (ticksCountReadLockStamp == 0 && readIteration >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              if (trySharedLockAttempts > 0) {
                trySharedLockAttempts--;
                ticksCountReadLockStamp = ticksCountLock.trySharedLock();
              } else {
                ticksCountReadLockStamp = ticksCountLock.sharedLock();
              }
            }
          }
          readIteration = Math.addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountReadLockStamp);
        }
      }
    }
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || readingDone;
    reading.setStartNanos(getStartNanos())
        .setTNanos(rightNanos)
        .setUnit(getSamplesInterval());
    return reading;
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long historyDurationNanos = getConfig().getHistoryLength() * getSamplesIntervalNanos();
      final long leftHistoryNanos = rightNanos - historyDurationNanos;
      if (NanosComparator.compare(leftHistoryNanos, tNanos) < 0) {//tNanos is ahead of or within the samples history
        @Nullable
        final TicksCounter existingSample;
        final long ticksCountExclusiveLockStamp;
        if (sequential) {
          ticksCountExclusiveLockStamp = 0;
        } else {
          assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ticksCountLock != null;
          ticksCountExclusiveLockStamp = ticksCountLock.isSharedLocked() ? ticksCountLock.lock() : 0;
        }
        try {
          final long timeSensitivityNanos = getTimeSensitivityNanos();
          if (timeSensitivityNanos == 1) {//TODO remove this if ?
            final TicksCounter newSample = getConfig().getTicksCounterSupplier()
                .apply(count);
            existingSample = samplesHistory.putIfAbsent(tNanos, newSample);
          } else {
            @Nullable
            final Entry<Long, TicksCounter> existingEntry = samplesHistory.floorEntry(tNanos);
            if (existingEntry != null && (tNanos - existingEntry.getKey()) < timeSensitivityNanos) {
              assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || NanosComparator.compare(existingEntry.getKey()
                  .longValue(), tNanos) <= 0;
              existingSample = existingEntry.getValue();
            } else {
              final TicksCounter newSample = getConfig().getTicksCounterSupplier()
                  .apply(count);
              existingSample = samplesHistory.putIfAbsent(tNanos, newSample);
            }
          }
        } finally {
          if (ticksCountExclusiveLockStamp != 0) {
            ticksCountLock.unlock(ticksCountExclusiveLockStamp);
          }
        }
        if (existingSample != null) {//we need to merge samples, and this can be safely done outside the exclusive ticksCountLock
          existingSample.add(count);
        }
      }
      getTicksTotalCounter().add(count);
      if (cleanRequired(rightNanos)) {
        clean(rightNanos);
      }
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    long rightNanos = rightSamplesWindowBoundary();
    final long effectiveRightNanos;
    if (NanosComparator.compare(tNanos, rightNanos) <= 0) {//tNanos is within or behind the samples window
      //TODO replace NanosComparator.compare with NanosComparator.max/min everywhere, not only here
      effectiveRightNanos = rightNanos;
    } else {//tNanos is ahead of the samples window
      effectiveRightNanos = tNanos;
    }
    return ConversionsAndChecks.rateAverage(effectiveRightNanos, getSamplesIntervalNanos(), getStartNanos(), ticksTotalCount());
  }

  /*The implementation of this method is an exact copy of rate(long, RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    double value;
    final boolean readingDone;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
    final long historyLengthNanos = getConfig().getHistoryLength() * samplesIntervalNanos;
    final long leftmostHistoryNanos = rightNanos - historyLengthNanos;
    if (NanosComparator.compare(effectiveLeftNanos, leftmostHistoryNanos) < 0) {
      //tNanos is behind the samples window, so return average over all samples
      value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
      readingDone = true;
    } else {//tNanos is within or ahead of the samples window
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        value = 0;
        readingDone = true;
      } else {
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          value = count;
          readingDone = true;
        } else {
          final long newRightNanos = rightSamplesWindowBoundary();
          final long newLeftmostHistoryNanos = newRightNanos - historyLengthNanos;
          if (NanosComparator.compare(newLeftmostHistoryNanos, effectiveLeftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but count is still correct
            value = count;
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
            readingDone = true;
          }
        }
      }
    }
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || readingDone;
    return value;
  }

  /**
   * @return {@inheritDoc}
   * The reading is not {@linkplain RateMeterReading#isAccurate() accurate} in cases when the method returns {@link #rateAverage()}.
   */
  @Override
  public final RateMeterReading rate(final long tNanos, final RateMeterReading reading) {//TODO use safe history concept
    checkArgument(tNanos, "tNanos");
    checkNotNull(reading, "reading");
    reading.setStartNanos(getStartNanos())
        .setTNanos(tNanos)
        .setUnit(getSamplesInterval());
    reading.setAccurate(true);//TODO use chaining
    final boolean readingDone;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
    final long historyLengthNanos = getConfig().getHistoryLength() * samplesIntervalNanos;
    final long leftmostHistoryNanos = rightNanos - historyLengthNanos;
    if (NanosComparator.compare(effectiveLeftNanos, leftmostHistoryNanos) < 0) {
      //tNanos is behind the samples window, so return average over all samples
      reading.setTNanos(rightNanos);
      reading.setAccurate(false);
      final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
      reading.setValue(value);
      readingDone = true;
    } else {//tNanos is within or ahead of the samples window
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        reading.setValue(0);
        readingDone = true;
      } else {
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {
          final long newRightNanos = rightSamplesWindowBoundary();
          final long newLeftmostHistoryNanos = newRightNanos - historyLengthNanos;
          if (NanosComparator.compare(newLeftmostHistoryNanos, effectiveLeftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but count is still correct
            reading.setValue(count);
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            reading.setTNanos(newRightNanos);
            reading.setAccurate(false);
            final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
            reading.setValue(value);
            readingDone = true;
          }
        }
      }
    }
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || readingDone;
    return reading;
  }

  /**
   * @return An {@linkplain Optional#empty() empty} {@link Optional}.
   */
  @Override
  public final Optional<Void> stats() {
    return Optional.empty();
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    return samplesHistory.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(TicksCounter::get)
        .sum();
  }

  private final boolean cleanRequired(final long rightSamplesWindowBoundary) {
    final long shiftNanos;
    if (sequential) {
      shiftNanos = rightSamplesWindowBoundary - cleanLastRightSamplesWindowBoundary;
    } else {
      shiftNanos = rightSamplesWindowBoundary - volatileCleanLastRightSamplesWindowBoundary;
    }
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final boolean result;
    if (shiftNanos > samplesIntervalNanos) {
      final long historyDurationNanos = getConfig().getHistoryLength() * samplesIntervalNanos;
      result = shiftNanos > (historyDurationNanos / 2);
    } else {
      result = false;
    }
    return result;
  }

  private final void clean(final long rightSamplesWindowBoundary) {
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || sequential || atomicCleanInProgress != null;
    if (sequential || atomicCleanInProgress.compareAndSet(false, true)) {
      try {
        if (sequential) {
          cleanLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        } else {
          volatileCleanLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        }
        final long historyDurationNanos = getConfig().getHistoryLength() * getSamplesIntervalNanos();
        final long leftHistoryNanos = rightSamplesWindowBoundary - historyDurationNanos;
        @Nullable
        final Long firstNanos = samplesHistory.firstKey();
        if (firstNanos != null && NanosComparator.compare(firstNanos.longValue(), leftHistoryNanos) < 0) {
          samplesHistory.subMap(firstNanos, true, leftHistoryNanos, false)
              .clear();
        }
      } finally {
        if (!sequential) {
          atomicCleanInProgress.set(false);
        }
      }
    }
  }
}