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

package stincmale.ratmex.executor.auxiliary;

import stincmale.ratmex.doc.ThreadSafe;

/**
 * This interface provides an abstraction over a physical notion of time and duration, thus allowing a full control over it (e.g. in tests).
 */
@ThreadSafe
public interface Time {
  /**
   * The semantics of this method is the same as of {@link System#nanoTime()}.
   */
  default long nanoTime() {
    return System.nanoTime();
  }
}