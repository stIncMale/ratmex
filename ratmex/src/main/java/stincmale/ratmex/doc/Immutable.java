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
import java.util.Collection;
import java.util.Collections;

/**
 * An object of a type annotated with this annotation is immutable, the type is also called immutable.
 * Immutable objects are inherently {@linkplain ThreadSafe thread-safe}.
 * <p>
 * <ul>
 * <li>When this annotation is applied to an interface it means that implementations of the interface must be immutable.
 * If an implementation of the interface is not immutable, this must be explicitly stated
 * (e.g. by annotating it with {@link NotThreadSafe @NotThreadSafe}).
 * One should never apply this annotation to an interface unless there is a very good reason for this.</li>
 * <li>When this annotation is applied to a not final class it may mean either of the following and must be further clarified by the class:
 * <ul><li>Subclasses of the class must be immutable.
 * If a subclass is not immutable, this must be explicitly stated (e.g. by annotating it with {@link NotThreadSafe @NotThreadSafe}).</li>
 * <li>The class is immutable, but it does not impose this restriction on its subclasses.
 * This interpretation must not be used by not final not abstract classes.</li></ul></li>
 * </ul>
 * <p>
 * Immutability of an object means that its state cannot be seen to change by a user<sup>(1)</sup>.
 * Of necessity (but not sufficient) this means that
 * <ul>
 * <li>all public fields are final;</li>
 * <li>all public final reference fields refer to immutable objects;</li>
 * <li>methods do not publish references to any internal state which is mutable by implementation even if not by design.</li>
 * </ul>
 * <p>
 * Immutable objects may still have internal mutable state (e.g. for purposes of performance optimization);
 * some state variables may be lazily computed,
 * so long as they are computed from immutable state and that users cannot tell the difference<sup>(2)</sup>
 * <p>
 * References to immutable objects may be passed between threads (published) without any additional synchronization on the part of a user.
 * <p>
 * Immutable objects must not be confused with read-only objects, which do not allow changing their state directly via their API
 * but still may display mutability of the state
 * (e.g. an object returned by {@link Collections#unmodifiableCollection(Collection)} cannot be modified via its API,
 * but because such an object is just a view of an original {@link Collection},
 * its state may be changed because of changes in the original {@link Collection}).
 * <p>
 * No type can be considered immutable unless the type is explicitly documented as such (e.g. it is annotated with {@link Immutable @Immutable}),
 * or immutability of the class follows of necessity from some well-known truth.
 * <p>
 * <sup>(1)</sup> By "use" we mean any interaction with an object via its API.
 * Interactions via any other means (e.g. via Java Reflection) is out of the scope of this specification.
 * Using a reference to an object (e.g. passing it between threads) is not considered to be a use of the object
 * and generally requires an additional synchronization unless the object is immutable.
 * <p>
 * <sup>(2)</sup> The paragraph is based on the Brian Goetz's very good specification of an immutable object from his book
 * <a href="http://jcip.net">"Java Concurrency in Practice"</a>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Immutable {
}