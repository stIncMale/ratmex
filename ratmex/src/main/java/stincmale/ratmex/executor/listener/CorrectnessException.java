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

package stincmale.ratmex.executor.listener;

import stincmale.ratmex.doc.Nullable;

/**
 * See {@link RateListener#onMeasurement(RateMeasuredEvent)}.
 */
public class CorrectnessException extends RuntimeException {
  private static final long serialVersionUID = -5116482860213079312L;

  /**
   * @param message See {@link #getMessage()}.
   */
  public CorrectnessException(@Nullable final String message) {
    super(message);
  }
}