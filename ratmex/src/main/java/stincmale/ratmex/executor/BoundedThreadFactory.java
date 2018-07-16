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
import java.util.concurrent.atomic.AtomicInteger;
import stincmale.ratmex.doc.Nullable;
import stincmale.ratmex.doc.ThreadSafe;
import static stincmale.ratmex.internal.util.Preconditions.checkArgument;
import static stincmale.ratmex.internal.util.Preconditions.checkNotNull;

@ThreadSafe
final class BoundedThreadFactory implements ThreadFactory {
  private final ThreadFactory threadFactory;
  private final int max;
  private final AtomicInteger counter;

  BoundedThreadFactory(final ThreadFactory threadFactory, final int maxThreads) {
    checkNotNull(threadFactory, "threadFactory");
    checkArgument(maxThreads >= 0, "maxThreads", "Must not be negative");
    this.threadFactory = threadFactory;
    max = maxThreads;
    counter = new AtomicInteger(0);
  }

  @Nullable
  @Override
  public final Thread newThread(@Nullable final Runnable r) {
    @Nullable
    final Thread result;
    if (counter.get() < max) {//omit CAS when possible (similar to DCL idiom)
      if (counter.incrementAndGet() <= max) {
        result = threadFactory.newThread(r);
      } else {
        result = null;
      }
    } else {
      result = null;
    }
    return result;
  }
}