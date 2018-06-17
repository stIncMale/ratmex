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

package stincmale.ratmex.internal.util;

import java.util.function.Supplier;
import stincmale.ratmex.doc.Nullable;
import static stincmale.ratmex.internal.util.Utils.format;

public final class Preconditions {
  public static final <T> T checkNotNull(
      @Nullable final T argument,
      final String safeParamName) throws NullPointerException {
    if (argument == null) {
      throw new NullPointerException(format("The argument %s must not be null", safeParamName));
    }
    return argument;
  }

  public static final void checkArgument(
      final boolean checkArgumentExpression,
      final String safeParamName,
      final String safeParamRestrictionDescription) throws IllegalArgumentException {
    if (!checkArgumentExpression) {
      throw new IllegalArgumentException(
          format("The argument %s is illegal. %s", safeParamName, safeParamRestrictionDescription));
    }
  }

  public static final void checkArgument(
      final boolean checkArgumentExpression,
      final String safeParamName,
      final Supplier<String> safeParamRestrictionDescriptionSupplier) throws IllegalArgumentException {
    if (!checkArgumentExpression) {
      throw new IllegalArgumentException(
          format("The argument %s is illegal. %s", safeParamName, safeParamRestrictionDescriptionSupplier.get()));
    }
  }

  private Preconditions() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}