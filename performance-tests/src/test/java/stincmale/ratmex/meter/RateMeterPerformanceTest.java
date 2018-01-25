package stincmale.ratmex.meter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import static stincmale.ratmex.util.Utils.format;

@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_METHOD)
public class RateMeterPerformanceTest {
  private static final long ACCEPTABLE_INCORRECTLY_REGISTERED_TICKS_EVENTS_COUNT_PER_TRIAL = 0;
  private static final String SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR = "groupOfRunsDescriptor";
  private static final Duration samplesInterval = Duration.of(1, ChronoUnit.MILLIS);
  private static final Integer[] numbersOfThreads = new Integer[] {1, 2, 4, 8, 16, 32};
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
  private static final SortedSet<GroupOfRunsDescriptor> groupOfRunsDescriptors;
  /*We supply groupOfRunsDescriptor to JMH via the static field groupOfRunsDescriptor,
    and hence we must be sure that this field refers to the same GroupOfRunsDescriptor during the test.*/
  private static final AtomicReference<GroupOfRunsDescriptor> groupOfRunsDescriptor;

  static {
    groupOfRunsDescriptors = new TreeSet<>();
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.NAVIGABLE_MAP_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.CONCURRENT_NAVIGABLE_MAP_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.RING_BUFFER_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.CONCURRENT_RING_BUFFER_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.CONCURRENT_SIMPLE_RATE_METER_WITH_DEFAULT_RING_BUFFER_RATE_METER_AND_STAMPED_LOCK_STRATEGY);
    groupOfRunsDescriptor = new AtomicReference<>(groupOfRunsDescriptors.first());
  }

  private enum GroupOfRunsDescriptor {
    NAVIGABLE_MAP_RATE_METER_DEFAULT(
        format("Default %s performance test", NavigableMapRateMeter.class.getSimpleName()),
        new Integer[] {1},
        startNanos -> new NavigableMapRateMeter(startNanos, samplesInterval)),
    CONCURRENT_NAVIGABLE_MAP_RATE_METER_DEFAULT(
        format("Default %s performance test", ConcurrentNavigableMapRateMeter.class.getSimpleName()),
        RateMeterPerformanceTest.numbersOfThreads,
        startNanos -> new ConcurrentNavigableMapRateMeter(startNanos, samplesInterval)),
    RING_BUFFER_RATE_METER_DEFAULT(
        format("Default %s performance test", RingBufferRateMeter.class.getSimpleName()),
        new Integer[] {1},
        startNanos -> new RingBufferRateMeter(startNanos, samplesInterval)),
    CONCURRENT_RING_BUFFER_RATE_METER_DEFAULT(
        format("Default %s performance test", ConcurrentRingBufferRateMeter.class.getSimpleName()),
        RateMeterPerformanceTest.numbersOfThreads,
        startNanos -> new ConcurrentRingBufferRateMeter(startNanos, samplesInterval)),
    CONCURRENT_SIMPLE_RATE_METER_WITH_DEFAULT_RING_BUFFER_RATE_METER_AND_STAMPED_LOCK_STRATEGY(
        format("%s with default %s performance test", ConcurrentSimpleRateMeter.class.getSimpleName(), RingBufferRateMeter.class.getSimpleName()),
        RateMeterPerformanceTest.numbersOfThreads,
        startNanos -> new ConcurrentSimpleRateMeter<>(RING_BUFFER_RATE_METER_DEFAULT.rateMeterCreator.apply(startNanos), new StampedLockStrategy()));

    private final String description;
    private final Set<Integer> numbersOfThreads;
    private final Function<Long, ? extends RateMeter<?>> rateMeterCreator;

    GroupOfRunsDescriptor(
        final String name,
        final Integer[] numbersOfThreads,
        final Function<Long, ? extends RateMeter<?>> rateMeterCreator) {
      this.description = name;
      @SuppressWarnings("all")//prevent an IDE from warning us about not using JDK 9 API here
      final Set<Integer> numberOfThreads = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(numbersOfThreads)));
      this.numbersOfThreads = numberOfThreads;
      this.rateMeterCreator = rateMeterCreator;
    }
  }

  public RateMeterPerformanceTest() {
  }

  @Test
  public final void run() {
    GroupOfRunsDescriptor guard = groupOfRunsDescriptors.first();
    for (final GroupOfRunsDescriptor groupOfRunsDescriptor : groupOfRunsDescriptors) {
      if (RateMeterPerformanceTest.groupOfRunsDescriptor.compareAndSet(guard, groupOfRunsDescriptor)) {
        run(groupOfRunsDescriptor);
        guard = groupOfRunsDescriptor;
      } else {
        /*We supply groupOfRunsDescriptor to JMH via the static field groupOfRunsDescriptor,
          and hence we must be sure that this field refers to the same GroupOfRunsDescriptor during the test.*/
        throw new RuntimeException("Concurrent execution of the test");
      }
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
      final GroupOfRunsDescriptor groupOfRunsDescriptor = GroupOfRunsDescriptor.valueOf(System.getProperty(SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR));
      rateMeter = groupOfRunsDescriptor.rateMeterCreator.apply(nanoTime());
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

  private static ConcurrentMap<GroupOfRunsDescriptor, Collection<RunResult>> testResults;

  @BeforeAll
  public static final void beforeAll() {
    testResults = new ConcurrentHashMap<>();
  }

  @AfterAll
  public static final void afterAll() {
    testResults.forEach((groupOfRunsDescriptor, runResults) -> {
      final String testId = groupOfRunsDescriptor.name();
      new JmhPerformanceTestResult(testId, RateMeterPerformanceTest.class, runResults).save();
      if (!Utils.isHeadless()) {
        final PerformanceTestResult ptr = new PerformanceTestResult(testId, RateMeterPerformanceTest.class)
            .load();
      }
    });
  }

  private final void run(final GroupOfRunsDescriptor groupOfRunsDescriptor) {
    for (int numberOfThreads : groupOfRunsDescriptor.numbersOfThreads) {
      final Collection<RunResult> runResults = runThroughput(groupOfRunsDescriptor, numberOfThreads);
      runResults.addAll(runLatency(groupOfRunsDescriptor, numberOfThreads));
      testResults.merge(groupOfRunsDescriptor, runResults, (oldValue, value) -> {
        final ArrayList<RunResult> result = new ArrayList<>(oldValue);
        result.addAll(value);
        return result;
      });
    }
  }

  private final Collection<RunResult> runThroughput(final GroupOfRunsDescriptor groupOfRunsDescriptor, final int numberOfThreads) {
    final Collection<RunResult> runResults;
    try {
      runResults = new Runner(JmhOptions.includingClass(getClass())
          .jvmArgsAppend(format("-D%s=%s", SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR, groupOfRunsDescriptor.name()))
          .mode(Mode.Throughput)
          .timeUnit(TimeUnit.MICROSECONDS)
          .threads(numberOfThreads)
          .build())
          .run();
    } catch (final RunnerException e) {
      throw new RuntimeException(e);
    }
    return runResults;
  }

  private final Collection<RunResult> runLatency(final GroupOfRunsDescriptor groupOfRunsDescriptor, final int numberOfThreads) {
    final Collection<RunResult> runResults;
    try {
      runResults = new Runner(JmhOptions.includingClass(getClass())
          .jvmArgsAppend(format("-D%s=%s", SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR, groupOfRunsDescriptor.name()))
          .mode(Mode.AverageTime)
          .timeUnit(TimeUnit.NANOSECONDS)
          .threads(numberOfThreads)
          .build())
          .run();
    } catch (final RunnerException e) {
      throw new RuntimeException(e);
    }
    return runResults;
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