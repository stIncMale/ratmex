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
import java.util.function.Supplier;
import stincmale.ratmex.meter.RateMeterConfig.Builder;

abstract class AbstractRateMeterTest<B extends Builder, C extends RateMeterConfig> {
  private final Supplier<B> rateMeterConfigBuilderSupplier;
  private final RateMeterCreator<C> rateMeterCreator;

  protected AbstractRateMeterTest(
      final Supplier<B> rateMeterConfigBuilderSupplier,
      final RateMeterCreator<C> rateMeterCreator) {
    this.rateMeterConfigBuilderSupplier = rateMeterConfigBuilderSupplier;
    this.rateMeterCreator = rateMeterCreator;
  }

  protected final Supplier<B> getRateMeterConfigBuilderSupplier() {
    return rateMeterConfigBuilderSupplier;
  }

  protected final RateMeterCreator<C> getRateMeterCreator() {
    return rateMeterCreator;
  }

  protected interface RateMeterCreator<C> {
    RateMeter<?> create(long startNanos, Duration samplesInterval, C config);
  }
}