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

package stincmale.ratmex.meter.auxiliary;

/**
 * An array abstraction which allows implementations with different semantics.
 */
public interface LongArray {
  /**
   * @return The length of this array. The length can never be changed once this object is constructed.
   */
  int length();

  /**
   * Sets the element at position {@code idx} to the given {@code value}.
   *
   * @param idx The index.
   * @param value The new value.
   */
  void set(int idx, long value);

  /**
   * Gets the current value at position {@code idx}.
   *
   * @param idx The index.
   *
   * @return The current value.
   */
  long get(int idx);

  /**
   * Adds the given {@code delta} to the element at index {@code idx}.
   *
   * @param idx The index.
   * @param delta The value to add.
   */
  void add(int idx, long delta);
}