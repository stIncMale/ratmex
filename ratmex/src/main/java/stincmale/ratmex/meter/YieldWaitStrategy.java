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

import java.util.function.BooleanSupplier;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This implementation of {@link WaitStrategy} just spins calling {@link Thread#yield()}
 * between tests of a {@linkplain #await(BooleanSupplier) condition}.
 */
@ThreadSafe
public final class YieldWaitStrategy implements WaitStrategy {
  private static final YieldWaitStrategy instance = new YieldWaitStrategy();

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link YieldWaitStrategy}.
   */
  public static final YieldWaitStrategy instance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    checkNotNull(condition, "condition");
    while (!condition.getAsBoolean()) {
      //      Thread.onSpinWait(); TODO multi-release JAR ?
      Thread.yield();
    }
  }

  private YieldWaitStrategy() {
  }
}