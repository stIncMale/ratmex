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

/**
 * A counter of ticks that can be used by {@link RateMeter} implementations.
 */
public interface TicksCounter {
  /**
   * Changes the {@linkplain #get() value} of this counter by {@code delta}.
   */
  void add(long delta);

  /**
   * @return The value of this counter.
   */
  long get();
}