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

/**
 * Contains library tools and utilities for internal use.
 * <p>
 * With the introduction of <a href="https://www.jcp.org/en/jsr/detail?id=376">Java Platform Module System in Java 9</a>,
 * we have a standard simple way to hide packages like this one from the external world.
 * Users with previous versions of Java must not use contents of this package or any of its subpackages.
 */
package stincmale.ratmex.internal.util;