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
 * An object of a type annotated with this annotation can be used<sup>(1)</sup> concurrently
 * without any additional synchronization or coordination on the part of a user. Such objects and types are called thread-safe.
 * <p>
 * <ul>
 * <li>When this annotation is applied to an interface it means that implementations of the interface must be thread-safe.
 * If an implementation of the interface is not thread-safe, this must be explicitly stated
 * (e.g. by annotating it with {@link NotThreadSafe @NotThreadSafe}).</li>
 * <li>When this annotation is applied to a not final class
 * it may mean either of the following and must be further clarified by the class:
 * <ul><li>Subclasses of the class must be thread-safe.
 * If a subclass is not thread-safe, this must be explicitly stated (e.g. by annotating it with {@link NotThreadSafe @NotThreadSafe}).</li>
 * <li>The class is thread-safe, but it does not impose this restriction on its subclasses.
 * This interpretation must not be used by not final not abstract classes.</li></ul></li>
 * </ul>
 * <p>
 * No type can be considered thread-safe unless the type is explicitly documented as such (e.g. it is annotated with {@link ThreadSafe @ThreadSafe}),
 * or thread-safety of the class follows of necessity from some well-known truth.
 * <p>
 * <sup>(1)</sup> By "use" we mean any interaction with an object via its API.
 * Interactions via any other means (e.g. via Java Reflection) is out of the scope of this specification.
 * Using a reference to an object (e.g. passing it between threads) is not considered to be a use of the object
 * and generally requires additional synchronization unless the object is {@linkplain Immutable immutable}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ThreadSafe {
}