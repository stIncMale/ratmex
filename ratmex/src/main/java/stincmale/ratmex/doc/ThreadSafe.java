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
 * <b>For objects</b><br>
 * An object of a type annotated with this annotation can be used<sup>(1)</sup> concurrently
 * without additional synchronization or coordination on the part of a user and will still behave correctly;
 * such objects and types are called thread-safe.
 * The correctness condition should be specified by the class,
 * and probably the most popular choice is <a href="https://cs.brown.edu/~mph/HerlihyW90/p463-herlihy.pdf">linearizability</a>.
 * <ul>
 * <li>
 * When this annotation is applied to an interface it means that implementations of the interface must be thread-safe.
 * If despite this requirement an implementation of the interface is not thread-safe, this must be explicitly stated
 * (e.g. by annotating it with {@link NotThreadSafe}).
 * </li>
 * <li>
 * When this annotation is applied to an abstract class
 * it may mean either of the following and must be further clarified by the class:
 * <ul>
 * <li>
 * Subclasses of the class must be thread-safe.
 * If despite this requirement a subclass is not thread-safe, this must be explicitly stated (e.g. by annotating it with {@link NotThreadSafe}).
 * </li>
 * <li>
 * The class is thread-safe, but does not impose this restriction on its subclasses.
 * </li>
 * </ul>
 * </li>
 * </ul>
 * <b>For methods</b><br>
 * A method annotated with this annotation can be used concurrently
 * without additional synchronization or coordination on the part of a user and will still behave correctly;
 * such methods are called thread-safe.
 * <ul>
 * <li>
 * When this annotation is applied to an abstract method it means that implementations of the method must be thread-safe.
 * If despite this requirement an implementation of the method is not thread-safe, this must be explicitly stated
 * (e.g. by annotating it with {@link NotThreadSafe}).
 * </li>
 * </ul>
 * No type or method can be considered thread-safe unless it is explicitly documented as such (e.g. it is annotated with {@link ThreadSafe}),
 * or thread-safety follows of necessity from a well-known truth.
 * <p>
 * <sup>(1)</sup> By "use" we mean interaction with an object via its API.
 * Interactions via other means (e.g. via Java Reflection) is out of the scope of this specification.
 * Using a reference to an object (e.g. passing it between threads) is not considered to be a use of the object
 * and generally requires additional synchronization.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface ThreadSafe {
}
