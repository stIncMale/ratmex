![RatMeX logo](https://stincmale.github.io/ratmex/logo-small.png)
# RatMeX
![Java requirement](https://img.shields.io/badge/Java-8+-blue.svg)
[![Docs link](https://img.shields.io/badge/docs-current-blue.svg)](https://github.com/stIncMale/ratmex/wiki)
[![API docs](https://img.shields.io/badge/javadocs-current-blue.svg)](https://stincmale.github.io/ratmex/apidocs/current/index.html?overview-summary.html)

A Java library that supplies a rate meter and a **Rat**e-**M**easuring **eX**ecutor based on it. This library is designed to be:
1. [high-performance](https://github.com/stIncMale/ratmex/wiki/Performance) ([`ConcurrentRingBufferRateMeter`](https://stincmale.github
.io/ratmex/apidocs/current/stincmale/ratmex/meter/ConcurrentRingBufferRateMeter.html) is able to register **23+ millions of concurrent ticks per second** with less than **180ns latency per registration including time spend calling [`System.nanoTime()`](https://docs.oracle.com/javase/9/docs/api/java/lang/System.html#nanoTime--)**)
2. garbage collector friendly ([`ConcurrentRingBufferRateMeter`](https://stincmale.github
.io/ratmex/apidocs/current/stincmale/ratmex/meter/ConcurrentRingBufferRateMeter.html) does not produce garbage)
3. free of external dependencies

**This is still a work in progress.**

***

Copyright 2018 [Valiantsin Kavalenka](https://sites.google.com/site/aboutmale/)

Licensed under the Apache License, Version 2.0 (the "License") (except where another license is explicitly specified);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.