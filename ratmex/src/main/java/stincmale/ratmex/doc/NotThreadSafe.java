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

package stincmale.ratmex.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An object of a type annotated with this annotation should not be used concurrently
 * without any additional synchronization or coordination on the part of a user. Such objects and types are called not thread-safe.
 * <p>
 * This annotation can be useful in cases specified by {@link ThreadSafe @ThreadSafe}
 * as well as in some cases where an explicit statement about the lack of thread-safety can help avoid misunderstanding.
 * <p>
 * This annotation does not forbid a type to be thread-safe
 * but rather informs a user that it may be not safe to use an object of the type concurrently
 * without any additional synchronization or coordination on the part of the user.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface NotThreadSafe {
}