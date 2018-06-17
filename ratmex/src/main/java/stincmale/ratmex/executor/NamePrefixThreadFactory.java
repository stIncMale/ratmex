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

package stincmale.ratmex.executor;

import java.util.concurrent.ThreadFactory;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

@ThreadSafe
final class NamePrefixThreadFactory implements ThreadFactory {
  private final ThreadFactory threadFactory;
  private final String namePrefix;

  NamePrefixThreadFactory(final ThreadFactory threadFactory, final String namePrefix) {
    checkNotNull(threadFactory, "threadFactory");
    checkNotNull(namePrefix, "namePrefix");
    this.threadFactory = threadFactory;
    this.namePrefix = namePrefix;
  }

  @Nullable
  @Override
  public final Thread newThread(final Runnable r) {
    @Nullable
    final Thread result = threadFactory.newThread(r);
    if (result != null) {
      result.setName(namePrefix + result.getName());
    }
    return result;
  }
}