package stincmale.ratmex.meter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import stincmale.ratmex.util.JmhOptions;
import stincmale.ratmex.util.JmhPerformanceTestResult;
import stincmale.ratmex.util.PerformanceTestTag;
import stincmale.ratmex.util.PerformanceTestResult;
import stincmale.ratmex.util.Utils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.ratmex.util.JmhOptions.DRY_RUN;

@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_METHOD)
public class RateMeterPerformanceTest {
  private static final long ACCEPTABLE_INCORRECTLY_REGISTERED_TICKS_EVENTS_COUNT_PER_TRIAL = 0;
  private static final Duration samplesInterval = Duration.of(1, ChronoUnit.MILLIS);
  private static final Set<Integer> numbersOfThreads = new HashSet<>(Arrays.asList(1, 1));
  @SuppressWarnings("rawtypes")
  private static final Class<? extends RateMeter> rateMeterClass = ConcurrentRingBufferRateMeter.class;
  private static final Supplier<ChainedOptionsBuilder> jmhOptions = () -> {
    final ChainedOptionsBuilder result = JmhOptions.get();
    if (!DRY_RUN) {
      result.warmupTime(milliseconds(750))
          .warmupIterations(3)
          .measurementTime(milliseconds(1000))
          .measurementIterations(3)
          .forks(3);
    }
    return result;
  };
  @SuppressWarnings("rawtypes")
  private static final Map<Class<? extends RateMeter>, Function<Long, RateMeter<?>>> rateMeterSuppliers;

  static {
    rateMeterSuppliers = new HashMap<>();
    rateMeterSuppliers.put(
        NavigableMapRateMeter.class,
        startNanos -> new NavigableMapRateMeter(startNanos, samplesInterval, NavigableMapRateMeter.defaultConfig()));
    rateMeterSuppliers.put(
        ConcurrentNavigableMapRateMeter.class,
        startNanos -> new ConcurrentNavigableMapRateMeter(startNanos, samplesInterval, ConcurrentNavigableMapRateMeter.defaultConfig()));
    rateMeterSuppliers.put(
        RingBufferRateMeter.class,
        startNanos -> new RingBufferRateMeter(startNanos, samplesInterval, RingBufferRateMeter.defaultConfig()));
    rateMeterSuppliers.put(
        ConcurrentRingBufferRateMeter.class,
        startNanos -> new ConcurrentRingBufferRateMeter(
            startNanos,
            samplesInterval,
            ConcurrentRingBufferRateMeter.defaultConfig()
                .toBuilder()
                //                .setWaitStrategySupplier(YieldWaitStrategy::instance)
                //                .setLockStrategySupplier(() -> new SpinLockStrategy(YieldWaitStrategy.instance()))
                //                .setStrictTick(false)
                .build()));
    rateMeterSuppliers.put(
        ConcurrentSimpleRateMeter.class,
        startNanos -> new ConcurrentSimpleRateMeter<>(
            rateMeterSuppliers.get(RingBufferRateMeter.class)
                .apply(startNanos),
            new StampedLockStrategy()));
    rateMeterSuppliers.get(rateMeterClass);
  }

  public RateMeterPerformanceTest() {
  }

  @Test
  public void run() {
    for (int numberOfThreads : numbersOfThreads) {
      runThroughput(numberOfThreads);
      runLatency(numberOfThreads);
    }
  }

  @Benchmark
  public void tick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    tick(benchmarkState.rateMeter);
  }

  @Benchmark
  public void alternateRateTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    alternateRateTick(benchmarkState.rateMeter, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void rateWithLessThanOnePercentTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    rateWithLessThanOnePercentTick(benchmarkState.rateMeter, threadState.counter);
    threadState.counter++;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private long counter;
    private RateMeterReading reading;

    public ThreadState() {
      reading = new RateMeterReading();
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counter = 0;
    }
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private RateMeter<?> rateMeter;

    public BenchmarkState() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      rateMeter = rateMeterSuppliers.get(rateMeterClass)
          .apply(nanoTime());
    }

    @TearDown(Level.Trial)
    public final void tearDown() {
      rateMeter.stats()
          .ifPresent(statistics -> {
            if (statistics instanceof ConcurrentRateMeterStats) {
              final ConcurrentRateMeterStats stats = (ConcurrentRateMeterStats)statistics;
              //JUnit5 Assertions.assertEquals(double, double, double) requires positive delta, hence we have to use Double.MIN_VALUE
              final double acceptableIncorrectlyRegisteredTicksEventsCount =
                  ACCEPTABLE_INCORRECTLY_REGISTERED_TICKS_EVENTS_COUNT_PER_TRIAL + Double.MIN_VALUE;
              assertEquals(0, stats.incorrectlyRegisteredTicksEventsCount(), acceptableIncorrectlyRegisteredTicksEventsCount);
            }
          });
    }
  }

  private static Collection<RunResult> runResults;

  @BeforeAll
  public static final void beforeAll() {
    runResults = new CopyOnWriteArrayList<>();
  }

  @AfterAll
  public static final void afterAll() {
    final String testName = rateMeterClass.getSimpleName() + "PerformanceTest";
    new JmhPerformanceTestResult(testName, RateMeterPerformanceTest.class, runResults).save();
    if (!Utils.isHeadless()) {
      final PerformanceTestResult ptr = new PerformanceTestResult(testName, RateMeterPerformanceTest.class)
          .load();
    }
  }

  private final void runThroughput(final int numberOfThreads) {
    try {
      runResults.addAll(new Runner(JmhOptions.includingClass(getClass())
          .mode(Mode.Throughput)
          .timeUnit(TimeUnit.MICROSECONDS)
          .threads(numberOfThreads)
          .build())
          .run());
    } catch (final RunnerException e) {
      throw new RuntimeException(e);
    }
  }

  private final void runLatency(final int numberOfThreads) {
    try {
      runResults.addAll(new Runner(JmhOptions.includingClass(getClass())
          .mode(Mode.AverageTime)
          .timeUnit(TimeUnit.NANOSECONDS)
          .threads(numberOfThreads)
          .build())
          .run());
    } catch (final RunnerException e) {
      throw new RuntimeException(e);
    }
  }

  private static final long nanoTime() {
    return System.nanoTime();
  }

  private static final void tick(final RateMeter<?> rm) {
    rm.tick(1, nanoTime());
  }

  private static final long rate(final RateMeter<?> rm) {
    return rm.rate();
    //    bh.consume(rateMeter.rate(new RateMeterReading()));
    //    bh.consume(rateMeter.rate(samplesInterval));
    //    bh.consume(rateMeter.rate(rateMeter.rightSamplesWindowBoundary()));
  }

  private static final long rateWithLessThanOnePercentTick(final RateMeter<?> rm, final long counter) {
    final long result;
    if ((counter & 127) == 0) {//same as (counter % 128 == 0), i.e. test if counter is a multiple of 128
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = rate(rm);
    }
    return result;
  }

  private static final long alternateRateTick(final RateMeter<?> rm, final long counter) {
    final long result;
    if ((counter & 1) == 0) {//same as (counter % 2 == 0), i.e. test if counter is even
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = rate(rm);
    }
    return result;
  }
}