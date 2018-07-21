<img align="left" src="https://stincmale.github.io/ratmex/logo-small.png" alt="RatMeX logo">

# RatMeX
<p align="right">
<a href="http://www.oracle.com/technetwork/java/javase/overview/index.html"><img src="https://img.shields.io/badge/Java-8+-blue.svg" alt="Java requirement"></a>
<a href="https://github.com/stIncMale/ratmex/wiki"><img src="https://img.shields.io/badge/documentation-current-blue.svg" alt="Docs link"></a>
<a href="https://stincmale.github.io/ratmex/apidocs/current/index.html?overview-summary.html"><img src="https://img.shields.io/badge/javadocs-current-blue.svg" alt="API docs"></a>
</p>

## About
An [open source](https://opensource.org/osd) Java library that supplies a [rate meter](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/RateMeter.html)
and a [**Rat**e-**M**easuring **eX**ecutor](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/executor/RateMeasuringExecutorService.html).
This library is designed to be:

property | description
--- | ---
[high-performance](https://github.com/stIncMale/ratmex/wiki/Performance) | [`ConcurrentRingBufferRateMeter`](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/ConcurrentRingBufferRateMeter.html) is able to register 23_000_000 concurrent ticks per second with less than 180ns latency per registration including time spend calling [`System.nanoTime()`](https://docs.oracle.com/javase/10/docs/api/java/lang/System.html#nanoTime()).
garbage collector friendly | [`RateMeter`](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/RateMeter.html) API is GC-fliendly. [`ConcurrentRingBufferRateMeter`](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/ConcurrentRingBufferRateMeter.html) does not produce garbage when is used with [`SpinLockStrategy`](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/meter/SpinLockStrategy.html). [`SubmitterWorkerRateMeasuringExecutorService`](https://stincmale.github.io/ratmex/apidocs/current/stincmale/ratmex/executor/SubmitterWorkerRateMeasuringExecutorService.html) reasonably minimizes creating of garbage.
free of external dependencies | No third-party compile- or run-time dependencies are required for your project to use RatMeX. 

## Rationale
JDK provides us with [`ScheduledExecutorService.scheduleAtFixedRate`](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,long,long,java.util.concurrent.TimeUnit)),
which says the following regarding the task being scheduled:
_If any execution of this task takes longer than its period, then subsequent executions may start late, but will not concurrently execute_.
This tells us that `ScheduledExecutorService`:
* is allowed to execute tasks with a lower rate than the target, and there is no easy way to check what the actual rate is or to enforce the target rate
* executes a scheduled task serially, which means we cannot easily benefit from multithreading, and the rate is heavily limited by the time the task takes to complete

RatMeX overcomes both of the above shortcomings.

## Versioning
This project uses [semantic versioning](https://semver.org). The current version is `0.1.0-SNAPSHOT`.

---

Copyright 2015-2018 [Valiantsin Kavalenka](https://sites.google.com/site/aboutmale/)

Licensed under the Apache License, Version 2.0 (the "License") (except where another license is explicitly specified);
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
