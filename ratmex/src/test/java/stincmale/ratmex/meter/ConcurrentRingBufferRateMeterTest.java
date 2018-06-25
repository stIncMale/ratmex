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

import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig;
import stincmale.ratmex.meter.config.ConcurrentRateMeterConfig.Builder;

public final class ConcurrentRingBufferRateMeterTest extends AbstractRateMeterUnitTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentRingBufferRateMeterTest() {
    super(
        () -> (Builder)ConcurrentRingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentRingBufferRateMeter::new);
  }
}