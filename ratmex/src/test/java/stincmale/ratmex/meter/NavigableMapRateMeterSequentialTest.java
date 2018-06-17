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

import org.junit.jupiter.api.Tag;
import stincmale.ratmex.util.ConcurrencyTestTag;
import stincmale.ratmex.meter.RateMeterConfig.Builder;

@Tag(ConcurrencyTestTag.VALUE)
public final class NavigableMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterSequentialTest() {
    super(() -> NavigableMapRateMeter.defaultConfig()
            .toBuilder(),
        NavigableMapRateMeter::new, 1);
  }
}