package stincmale.ratmex.performance.meter;

import java.awt.Color;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
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
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.meter.ConcurrentNavigableMapRateMeter;
import stincmale.ratmex.meter.ConcurrentRateMeterConfig;
import stincmale.ratmex.meter.ConcurrentRateMeterStats;
import stincmale.ratmex.meter.ConcurrentRingBufferRateMeter;
import stincmale.ratmex.meter.NavigableMapRateMeter;
import stincmale.ratmex.meter.RateMeter;
import stincmale.ratmex.meter.RateMeterReading;
import stincmale.ratmex.meter.RingBufferRateMeter;
import stincmale.ratmex.meter.SpinLockStrategy;
import stincmale.ratmex.meter.YieldWaitStrategy;
import stincmale.ratmex.performance.util.JmhOptions;
import stincmale.ratmex.performance.util.JmhPerformanceTestResult;
import stincmale.ratmex.performance.util.PerformanceTestTag;
import stincmale.ratmex.performance.util.PerformanceTestResult;
import stincmale.ratmex.performance.util.Utils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static stincmale.ratmex.performance.util.JmhOptions.jvmArgsAppend;
import static stincmale.ratmex.performance.util.Utils.format;

@Tag(PerformanceTestTag.VALUE)
@TestInstance(Lifecycle.PER_METHOD)
public class RateMeterPerformanceTest {
  private static final long ACCEPTABLE_INCORRECTLY_REGISTERED_TICKS_EVENTS_COUNT_PER_TRIAL = 0;
  private static final String SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR = "groupOfRunsDescriptor";
  private static final Duration samplesInterval = Duration.of(10, ChronoUnit.MILLIS);
  private static final LinkedHashSet<GroupOfRunsDescriptor> groupOfRunsDescriptors;
  private static final AtomicReference<GroupOfRunsDescriptor> groupOfRunsDescriptor;

  static {
    groupOfRunsDescriptors = new LinkedHashSet<>();
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.NAVIGABLE_MAP_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.CONCURRENT_NAVIGABLE_MAP_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.RING_BUFFER_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.CONCURRENT_RING_BUFFER_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.CONCURRENT_RING_BUFFER_RATE_METER_RELAXED_TICKS);
    groupOfRunsDescriptors.add(
        GroupOfRunsDescriptor.CONCURRENT_SIMPLE_RATE_METER_WITH_DEFAULT_RING_BUFFER_RATE_METER_AND_SPIN_LOCK_STRATEGY_WITH_YIELD_WAIT_STRATEGY);
    groupOfRunsDescriptor = new AtomicReference<>(groupOfRunsDescriptors.stream()
        .findFirst()
        .get());
  }

  private enum GroupOfRunsDescriptor {
    NAVIGABLE_MAP_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", NavigableMapRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        Collections.singleton(1),
        startNanos -> new NavigableMapRateMeter(startNanos, samplesInterval)),
    CONCURRENT_NAVIGABLE_MAP_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", ConcurrentNavigableMapRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentNavigableMapRateMeter(startNanos, samplesInterval)),
    RING_BUFFER_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", RingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        Collections.singleton(1),
        startNanos -> new RingBufferRateMeter(startNanos, samplesInterval)),
    CONCURRENT_RING_BUFFER_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", ConcurrentRingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentRingBufferRateMeter(startNanos, samplesInterval)),
    CONCURRENT_RING_BUFFER_RATE_METER_RELAXED_TICKS(
        format("%s, relaxed ticks, samples interval: %sms", ConcurrentRingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentRingBufferRateMeter(
            startNanos,
            samplesInterval,
            ConcurrentRingBufferRateMeter.defaultConfig()
                .toBuilder()
                .setMode(ConcurrentRateMeterConfig.Mode.RELAXED_TICKS)
                .setCollectStats(true)
                .build())),
    CONCURRENT_SIMPLE_RATE_METER_WITH_DEFAULT_RING_BUFFER_RATE_METER_AND_SPIN_LOCK_STRATEGY_WITH_YIELD_WAIT_STRATEGY(
        format("%s with %s, spin LS & yield WS, samples interval: %sms",
            ConcurrentSimpleRateMeter.class.getSimpleName(), RingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentSimpleRateMeter<>(
            RING_BUFFER_RATE_METER_DEFAULT.rateMeterCreator.apply(startNanos), new SpinLockStrategy(YieldWaitStrategy.instance())));

    private final String description;
    private final Set<Integer> numbersOfThreads;
    private final Function<Long, ? extends RateMeter<?>> rateMeterCreator;

    GroupOfRunsDescriptor(
        final String name,
        final Collection<Integer> numbersOfThreads,
        final Function<Long, ? extends RateMeter<?>> rateMeterCreator) {
      this.description = name;
      this.numbersOfThreads = Collections.unmodifiableSet(new TreeSet<>(numbersOfThreads));
      this.rateMeterCreator = rateMeterCreator;
    }
  }

  public RateMeterPerformanceTest() {
  }

  @Test
  public final void run() {
    GroupOfRunsDescriptor guard = groupOfRunsDescriptors.stream()
        .findFirst()
        .get();
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
  public void alternateTicksCountTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    alternateTicksCountTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void ticksCountWithLessThanOnePercentTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    ticksCountWithLessThanOnePercentTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void alternateRateTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    alternateRateTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void rateWithLessThanOnePercentTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    rateWithLessThanOnePercentTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
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

    @TearDown(Level.Iteration)
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
    System.out.println();
    testResults.forEach((groupOfRunsDescriptor, runResults) -> {
      final String testId = groupOfRunsDescriptor.name();
      new JmhPerformanceTestResult(testId, RateMeterPerformanceTest.class, runResults).save();
    });
    generateCharts(PerformanceTestResult.loadAll(RateMeterPerformanceTest.class));
  }

  public static final void generateCharts(final Collection<? extends PerformanceTestResult> loadedPtrs) {
    if (!Utils.isHeadless()) {
      loadedPtrs.forEach(ptr -> {
        @Nullable
        GroupOfRunsDescriptor descriptor;
        try {
          descriptor = GroupOfRunsDescriptor.valueOf(ptr.getTestId());
        } catch (IllegalArgumentException e) {
          descriptor = null;
        }
        if (descriptor != null) {
          generateCharts(descriptor, ptr);
        }
      });
    }
  }

  private static final void generateCharts(final GroupOfRunsDescriptor groupOfRunsDescriptor, final PerformanceTestResult ptr) {
    final Map<String, Function<XYSeries, XYSeries>> benchmarkSeriesProcessors = new TreeMap<>();
    {
      benchmarkSeriesProcessors.put("tick", series -> (XYSeries)series.setMarker(SeriesMarkers.CIRCLE));
      benchmarkSeriesProcessors.put("alternateTicksCountTick", series -> (XYSeries)series.setMarker(SeriesMarkers.SQUARE));
      benchmarkSeriesProcessors.put("alternateRateTick", series -> (XYSeries)series.setMarker(SeriesMarkers.SQUARE));
      benchmarkSeriesProcessors.put("ticksCountWithLessThanOnePercentTick", series -> (XYSeries)series.setMarker(SeriesMarkers.DIAMOND));
      benchmarkSeriesProcessors.put("rateWithLessThanOnePercentTick", series -> (XYSeries)series.setMarker(SeriesMarkers.DIAMOND));
    }
    ptr.save(
        Mode.Throughput,
        groupOfRunsDescriptor.description,
        "number of threads",
        "throughput, ops/s",
        "###,###,###.### mln",
        benchmarkSeriesProcessors);
    ptr.save(
        Mode.AverageTime,
        groupOfRunsDescriptor.description,
        "number of threads",
        "latency, ns",
        null,
        benchmarkSeriesProcessors);
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
      runResults = new Runner(jvmArgsAppend(
          JmhOptions.includingClass(RateMeterPerformanceTest.class),
          format("-D%s=%s", SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR, groupOfRunsDescriptor.name()))
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
      runResults = new Runner(jvmArgsAppend(
          JmhOptions.includingClass(RateMeterPerformanceTest.class),
          format("-D%s=%s", SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR, groupOfRunsDescriptor.name()))
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

  private static final long ticksCount(final RateMeter<?> rm, final RateMeterReading reading) {
    return rm.ticksCount(new RateMeterReading())
        .getValueLong();
  }

  private static final double ticksCountWithLessThanOnePercentTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
    final double result;
    if ((counter & 127) == 0) {//same as (counter % 128 == 0), i.e. test if counter is a multiple of 128
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = ticksCount(rm, reading);
    }
    return result;
  }

  private static final double alternateTicksCountTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
    final double result;
    if ((counter & 1) == 0) {//same as (counter % 2 == 0), i.e. test if counter is even
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = ticksCount(rm, reading);
    }
    return result;
  }

  private static final double rate(final RateMeter<?> rm, final RateMeterReading reading) {
    return rm.rate(rm.rightSamplesWindowBoundary(), reading)
        .getValueDouble();
  }

  private static final double rateWithLessThanOnePercentTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
    final double result;
    if ((counter & 127) == 0) {//same as (counter % 128 == 0), i.e. test if counter is a multiple of 128
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = rate(rm, reading);
    }
    return result;
  }

  private static final double alternateRateTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
    final double result;
    if ((counter & 1) == 0) {//same as (counter % 2 == 0), i.e. test if counter is even
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = rate(rm, reading);
    }
    return result;
  }
}