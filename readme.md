![RatMeX logo](https://stincmale.github.io/ratmex/logo-small.png)
# RatMeX
A Java library that supplies a rate meter and a **Rat**e-**M**easuring **eX**ecutor based on it. This library is designed to be:
* high-performance ([ConcurrentRingBufferRateMeter](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/ConcurrentRingBufferRateMeter.html) is able to register 23+ millions of concurrent events per second with less than 180ns latency per registration)
* garbage collector friendly ([ConcurrentRingBufferRateMeter](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/ConcurrentRingBufferRateMeter.html) does not produce garbage)
* free of external dependencies

Documentation is available [here](https://github.com/stIncMale/ratmex/wiki).

**This library is still a work in progress.**

***

All content is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0), except where another license is explicitly specified.
The logo is taken from the [Clipart Library](http://clipart-library.com/clipart/3099.htm) and is free to use/belongs to public domain.

Copyright 2018 Valiantsin Kavalenka

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.