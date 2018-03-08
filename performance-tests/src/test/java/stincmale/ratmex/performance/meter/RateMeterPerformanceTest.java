package stincmale.ratmex.performance.meter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
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
@TestInstance(Lifecycle.PER_CLASS)
public class RateMeterPerformanceTest {
  private static final long ACCEPTABLE_INCORRECTLY_REGISTERED_TICKS_EVENTS_COUNT_PER_TRIAL = 0;
  private static final String SYSTEM_PROPERTY_GROUP_OF_RUNS_DESCRIPTOR = "groupOfRunsDescriptor";
  private static final Duration samplesInterval = Duration.of(10, ChronoUnit.MILLIS);
  private static final LinkedHashSet<GroupOfRunsDescriptor> groupOfRunsDescriptors;

  static {
    groupOfRunsDescriptors = new LinkedHashSet<>();
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.A_CONCURRENT_NAVIGABLE_MAP_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.B_CONCURRENT_RING_BUFFER_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.C_CONCURRENT_RING_BUFFER_RATE_METER_RELAXED_TICKS);
    groupOfRunsDescriptors.add(
        GroupOfRunsDescriptor.D_CONCURRENT_SIMPLE_RATE_METER_WITH_DEFAULT_RING_BUFFER_RATE_METER_AND_SPIN_LOCK_STRATEGY_WITH_YIELD_WAIT_STRATEGY);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.E_NAVIGABLE_MAP_RATE_METER_DEFAULT);
    groupOfRunsDescriptors.add(GroupOfRunsDescriptor.F_RING_BUFFER_RATE_METER_DEFAULT);
  }

  private enum GroupOfRunsDescriptor {
    E_NAVIGABLE_MAP_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", NavigableMapRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        Collections.singleton(1),
        startNanos -> new NavigableMapRateMeter(startNanos, samplesInterval)),
    A_CONCURRENT_NAVIGABLE_MAP_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", ConcurrentNavigableMapRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentNavigableMapRateMeter(startNanos, samplesInterval)),
    F_RING_BUFFER_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", RingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        Collections.singleton(1),
        startNanos -> new RingBufferRateMeter(startNanos, samplesInterval)),
    B_CONCURRENT_RING_BUFFER_RATE_METER_DEFAULT(
        format("%s, samples interval: %sms", ConcurrentRingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentRingBufferRateMeter(startNanos, samplesInterval)),
    C_CONCURRENT_RING_BUFFER_RATE_METER_RELAXED_TICKS(
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
    D_CONCURRENT_SIMPLE_RATE_METER_WITH_DEFAULT_RING_BUFFER_RATE_METER_AND_SPIN_LOCK_STRATEGY_WITH_YIELD_WAIT_STRATEGY(
        format("%s with %s, spin LS & yield WS, samples interval: %sms",
            ConcurrentSimpleRateMeter.class.getSimpleName(), RingBufferRateMeter.class.getSimpleName(), samplesInterval.toMillis()),
        JmhOptions.numbersOfThreads,
        startNanos -> new ConcurrentSimpleRateMeter<>(
            F_RING_BUFFER_RATE_METER_DEFAULT.rateMeterCreator.apply(startNanos), new SpinLockStrategy(YieldWaitStrategy.instance())));

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
    for (final GroupOfRunsDescriptor groupOfRunsDescriptor : groupOfRunsDescriptors) {
      new JmhPerformanceTestResult(groupOfRunsDescriptor.name(), run(groupOfRunsDescriptor)).save();
    }
  }

  @Benchmark
  public void tick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    tick(benchmarkState.rateMeter);
  }

  @Benchmark
  public void alternateRateTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    alternateRateTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void rateLessThanOnePercentTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    rateLessThanOnePercentTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void alternateRateExplicitTTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    alternateRateExplicitTTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
    threadState.counter++;
  }

  @Benchmark
  public void rateExplicitTLessThanOnePercentTick(final BenchmarkState benchmarkState, final ThreadState threadState) {
    rateExplicitTLessThanOnePercentTick(benchmarkState.rateMeter, threadState.reading, threadState.counter);
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
              final double acceptableIncorrectlyRegisteredTicksEventsCount =
                  ACCEPTABLE_INCORRECTLY_REGISTERED_TICKS_EVENTS_COUNT_PER_TRIAL + Double.MIN_VALUE;
              assertEquals(0, stats.incorrectlyRegisteredTicksEventsCount(), acceptableIncorrectlyRegisteredTicksEventsCount);
            }
          });
    }
  }

  @AfterAll
  public final void afterAll() {
    System.out.println();
    generateCharts();
  }

  public static final void generateCharts() {
    final Collection<PerformanceTestResult> loadedPtrs = PerformanceTestResult.load(testId -> testId.contains("RATE_METER"));
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
      PerformanceTestResult.createAggregate("AGGREGATE_RATE_METER", loadedPtrs)
          .ifPresent(aggregateTestResult -> {
            final String chartName = format("Comparison of rate meters");
            for (final int numberOfAvailableProcessors : JmhOptions.numbersOfThreads) {
              aggregateTestResult.save(
                  Mode.Throughput,
                  numberOfAvailableProcessors,
                  chartName,
                  "throughput, ops/s",
                  "###,###,###.### mln");
              aggregateTestResult.save(
                  Mode.AverageTime,
                  numberOfAvailableProcessors,
                  chartName,
                  "latency, ns",
                  null);
            }
          });
    }
  }

  private static final void generateCharts(final GroupOfRunsDescriptor groupOfRunsDescriptor, final PerformanceTestResult ptr) {
    final Map<String, Function<XYSeries, XYSeries>> benchmarkSeriesProcessors = new TreeMap<>();
    {
      benchmarkSeriesProcessors.put("tick", series -> (XYSeries)series.setMarker(SeriesMarkers.CIRCLE));
      benchmarkSeriesProcessors.put("alternateRateTick", series -> (XYSeries)series.setMarker(SeriesMarkers.SQUARE));
      benchmarkSeriesProcessors.put("alternateRateExplicitTTick", series -> (XYSeries)series.setMarker(SeriesMarkers.SQUARE));
      benchmarkSeriesProcessors.put("rateLessThanOnePercentTick", series -> (XYSeries)series.setMarker(SeriesMarkers.DIAMOND));
      benchmarkSeriesProcessors.put("rateExplicitTLessThanOnePercentTick", series -> (XYSeries)series.setMarker(SeriesMarkers.DIAMOND));
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

  private final Collection<RunResult> run(final GroupOfRunsDescriptor groupOfRunsDescriptor) {
    final int numberOfAvailableProcessors = Runtime.getRuntime()
        .availableProcessors();
    final Collection<RunResult> runResults = new ArrayList<>();
    for (int numberOfThreads : groupOfRunsDescriptor.numbersOfThreads) {
      runResults.addAll(runThroughput(groupOfRunsDescriptor, numberOfThreads));
      if (numberOfThreads <= numberOfAvailableProcessors) {
        runResults.addAll(runLatency(groupOfRunsDescriptor, numberOfThreads));
      }
    }
    return runResults;
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

  private static final long rate(final RateMeter<?> rm, final RateMeterReading reading) {
    return rm.rate(new RateMeterReading())
        .getValueLong();
  }

  private static final double rateLessThanOnePercentTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
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

  private static final double rateExplicitT(final RateMeter<?> rm, final RateMeterReading reading) {
    return rm.rate(rm.rightSamplesWindowBoundary(), reading)
        .getValueDouble();
  }

  private static final double rateExplicitTLessThanOnePercentTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
    final double result;
    if ((counter & 127) == 0) {//same as (counter % 128 == 0), i.e. test if counter is a multiple of 128
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = rateExplicitT(rm, reading);
    }
    return result;
  }

  private static final double alternateRateExplicitTTick(final RateMeter<?> rm, final RateMeterReading reading, final long counter) {
    final double result;
    if ((counter & 1) == 0) {//same as (counter % 2 == 0), i.e. test if counter is even
      rm.tick(1, nanoTime());
      result = 0;
    } else {
      result = rateExplicitT(rm, reading);
    }
    return result;
  }
}