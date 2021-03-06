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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import stincmale.ratmex.common.NanosComparator;
import stincmale.ratmex.meter.config.RateMeterConfig;
import stincmale.ratmex.meter.config.RateMeterConfig.Builder;
import static java.time.Duration.ofNanos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static stincmale.ratmex.internal.util.Utils.format;

@TestInstance(Lifecycle.PER_METHOD)
abstract class AbstractRateMeterConcurrencyTest<B extends Builder, C extends RateMeterConfig> extends AbstractRateMeterTest<B, C> {
  private final int numberOfThreads;
  private ExecutorService ex;

  protected AbstractRateMeterConcurrencyTest(
      final Supplier<B> rateMeterConfigBuilderSupplier,
      final RateMeterCreator<C> rateMeterCreator,
      final int numberOfThreads) {
    super(rateMeterConfigBuilderSupplier, rateMeterCreator);
    this.numberOfThreads = numberOfThreads;
  }

  @Test
  public final void test() {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final int numberOfTestIterations = 30_000;
    final int minNumberOfSamples = 300;
    final int maxNumberOfSamples = 2 * minNumberOfSamples;
    for (int i = 1; i <= numberOfTestIterations; i++) {
      final Duration samplesInterval = i == 1 ? ofNanos(1) : ofNanos(rnd.nextInt(1, 400));
      final TestParams tp = new TestParams(
          numberOfThreads,
          rnd.nextInt(minNumberOfSamples, maxNumberOfSamples),
          rnd.nextBoolean(),
          rnd.nextInt(0, 5),
          samplesInterval,
          rnd.nextBoolean());
      doTest(i, tp, ex);
    }
  }

  @BeforeEach
  public final void beforeEach() {
    ex = Executors.newFixedThreadPool(numberOfThreads);
  }

  @AfterEach
  public final void afterEach() {
    ex.shutdownNow();
  }

  private final void doTest(final int iterationIdx, final TestParams tp, final ExecutorService ex) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long startNanos = rnd.nextLong((long)(11.3d * Integer.MIN_VALUE), (long)(11.3d * Integer.MAX_VALUE));
    @SuppressWarnings("unchecked")
    final C rateMeterConfig = (C)getRateMeterConfigBuilderSupplier()
        .get()
        .setTimeSensitivity(ofNanos(1))
        .build();
    final RateMeter<?> rm = getRateMeterCreator().create(
        startNanos,
        tp.samplesInterval,
        rateMeterConfig);
    final TickGenerator tickGenerator = new TickGenerator(
        startNanos,
        startNanos + (long)(rnd.nextDouble(0, 500) * tp.samplesInterval.toNanos()),
        tp.sameSamplesForAllTickGenerators,
        tp.numberOfSamples,
        tp.numberOfThreads);
    final Collection<TickGenerator> tickGenerators = tickGenerator.split();
    assertEquals(tp.numberOfThreads, tickGenerators.size());
    final CountDownLatch latch = new CountDownLatch(tp.numberOfThreads);
    tickGenerators.stream()
        .map(ticksGenerator -> ticksGenerator.generate(rm, tp.orderTicksByTime, tp.tickToRateRatio, ex, latch))
        .collect(Collectors.toList())
        .forEach(futureGenerate -> {
          try {
            futureGenerate.get();
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        });
    assertEquals(tickGenerator.rightmostTNanos(), rm.rightSamplesWindowBoundary(), format("Iteration#%s, %s", iterationIdx, tp));
    assertEquals(tickGenerator.totalCount(), rm.ticksCountTotal(), format("Iteration#%s, %s", iterationIdx, tp));
    final RateMeterReading reading = new RateMeterReading();
    assertTrue(rm.rate(reading)
        .isAccurate(), format("Iteration#%s, %s", iterationIdx, tp));
    assertEquals(
        tickGenerator.countRightmost(tp.samplesInterval.toNanos()),
        rm.rate(reading)
            .getValueLong(),
        format("Iteration#%s, %s", iterationIdx, tp));
    assertEquals(
        tickGenerator.countRightmost(tp.samplesInterval.toNanos()),
        rm.rate(),
        format("Iteration#%s, %s", iterationIdx, tp));
    assertEquals(rm.rate(reading)
        .getTNanos(), rm.rightSamplesWindowBoundary(), format("Iteration#%s, %s", iterationIdx, tp));
    assertEquals(rm.rate(reading)
        .getValueLong(), rm.rate(), format("Iteration#%s, %s", iterationIdx, tp));
  }

  private static final class TestParams {
    final int numberOfThreads;
    final int numberOfSamples;
    final boolean orderTicksByTime;
    final int tickToRateRatio;
    final Duration samplesInterval;
    final boolean sameSamplesForAllTickGenerators;

    TestParams(
        final int numberOfThreads,
        final int numberOfSamples,
        final boolean orderTicksByTime,
        final int tickToRateRatio,
        final Duration samplesInterval,
        final boolean sameSamplesForAllTickGenerators) {
      this.numberOfThreads = numberOfThreads;
      this.numberOfSamples = numberOfSamples;
      this.orderTicksByTime = orderTicksByTime;
      this.tickToRateRatio = tickToRateRatio;
      this.samplesInterval = samplesInterval;
      this.sameSamplesForAllTickGenerators = sameSamplesForAllTickGenerators;
    }

    @Override
    public final String toString() {
      return getClass().getSimpleName() +
          "{numberOfThreads=" + numberOfThreads +
          ", numberOfSamples=" + numberOfSamples +
          ", orderTicksByTime=" + orderTicksByTime +
          ", tickToRateRatio=" + tickToRateRatio +
          ", samplesInterval=" + samplesInterval +
          ", sameSamplesForAllTickGenerators=" + sameSamplesForAllTickGenerators +
          '}';
    }
  }

  private final class TickGenerator {
    private final NavigableMap<Long, Long> samples;
    private final boolean sameSamplesForAllTickGenerators;
    private final int splitN;

    TickGenerator(
        final long startNanos,
        final long maxTNanosInclusive,
        final boolean sameSamplesForAllTickGenerators,
        final int numberOfSamples,
        final int splitN) {
      final ThreadLocalRandom rnd = ThreadLocalRandom.current();
      samples = new TreeMap<>(NanosComparator.instance());
      samples.put(startNanos, 0L);
      for (int i = 0; i < numberOfSamples - 1; i++) {
        final long tNanos = rnd.nextLong(startNanos, maxTNanosInclusive + 1);
        final long count = rnd.nextLong(-3, 4);
        samples.put(tNanos, count);
      }
      this.sameSamplesForAllTickGenerators = sameSamplesForAllTickGenerators;
      this.splitN = splitN;
    }

    TickGenerator(final NavigableMap<Long, Long> samples) {
      this.samples = samples;
      sameSamplesForAllTickGenerators = false;
      splitN = -1;
    }

    final Future<?> generate(
        final RateMeter<?> rm,
        boolean orderTicksByTime,
        int tickToRateRatio,
        final ExecutorService ex,
        final CountDownLatch latch) {
      final List<Entry<Long, Long>> shuffledSamples = new ArrayList<>(samples.entrySet());
      if (!orderTicksByTime) {
        Collections.shuffle(shuffledSamples);
      }
      return ex.submit(() -> {
        latch.countDown();
        try {
          if (!latch.await(1, TimeUnit.SECONDS)) {
            throw new RuntimeException("Time is out");
          }
        } catch (final InterruptedException e) {
          throw new RuntimeException("Unexpected interruption");
        }
        int i = 0;
        shuffledSamples.forEach(sample -> {
          rm.tick(sample.getValue(), sample.getKey());
          if (tickToRateRatio > 0 && i % tickToRateRatio == 0) {
            final int randomInt = ThreadLocalRandom.current()
                .nextInt(6);
            if (randomInt == 0) {
              rm.rate();
            } else if (randomInt == 1) {
              rm.rate(new RateMeterReading());
            } else if (randomInt == 2) {
              rm.rate();
            } else if (randomInt == 3) {
              rm.rate(new RateMeterReading());
            } else if (randomInt == 4) {
              rm.rate(rm.rightSamplesWindowBoundary());
            } else {
              rm.rate(rm.rightSamplesWindowBoundary(), new RateMeterReading());
            }
          }
        });
      });
    }

    final Collection<TickGenerator> split() {
      final List<NavigableMap<Long, Long>> splitSamples = new ArrayList<>(splitN);
      if (sameSamplesForAllTickGenerators) {
        for (int i = 0; i < splitN; i++) {
          splitSamples.add(new TreeMap<>(samples));
        }
      } else {
        for (int i = 0; i < splitN; i++) {
          splitSamples.add(new TreeMap<>(NanosComparator.instance()));
        }
        final List<Entry<Long, Long>> listOfSamples = new ArrayList<>(samples.entrySet());
        for (int i = 0; i < listOfSamples.size(); i++) {
          final Entry<Long, Long> sample = listOfSamples.get(i);
          splitSamples.get(i % splitSamples.size())
              .put(sample.getKey(), sample.getValue());
        }
      }
      return splitSamples.stream()
          .map(TickGenerator::new)
          .collect(Collectors.toList());
    }

    final long countRightmost(final long samplesIntervalNanos) {
      final long rightNanos = rightmostTNanos();
      final long leftNanos = rightNanos - samplesIntervalNanos;
      return samples.subMap(leftNanos, false, rightNanos, true)
          .values()
          .stream()
          .mapToLong(Long::longValue)
          .sum() * (sameSamplesForAllTickGenerators ? splitN : 1);
    }

    final long totalCount() {
      return samples.values()
          .stream()
          .mapToLong(Long::longValue)
          .sum() * (sameSamplesForAllTickGenerators ? splitN : 1);
    }

    final long rightmostTNanos() {
      final long leftmost = samples.firstKey();
      long result = samples.lastKey();
      while (samples.get(result) == 0 && result != leftmost) {
        result = samples.lowerKey(result);
      }
      return result;
    }
  }
}