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
 * An object of a type annotated with this annotation or a method annotated with this annotation should not be used<sup>(1)</sup> concurrently
 * without additional synchronization or coordination on the part of a user;
 * such objects, types and methods are called not thread-safe.
 * <p>
 * This annotation can be useful in cases specified by {@link ThreadSafe}
 * as well as in some cases where an explicit statement about the lack of thread-safety can help avoiding misunderstanding.
 * <p>
 * This annotation does not forbid a type to be thread-safe
 * but rather informs a user that it may be not safe to use an object or method concurrently
 * without additional synchronization or coordination on the part of the user.
 * <p>
 * <sup>(1)</sup> By "use" we mean interaction with an object via its API.
 * Interactions via other means (e.g. via Java Reflection) is out of the scope of this specification.
 * Using a reference to an object (e.g. passing it between threads) is not considered to be a use of the object
 * and generally requires additional synchronization.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface NotThreadSafe {
}
