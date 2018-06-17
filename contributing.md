# Build-related commands
This project uses [Maven](https://maven.apache.org/) for organizing, managing dependencies and build procedures.

Run from project root directory:

Command | Description
--- | ---
`mvn clean install -f root.xml` | Install root.xml artifact to local Maven repository (usually `~/.m2/repository/` directory).
`mvn clean install -f version.xml` |
`mvn clean install -f build.xml` |
`mvn clean test -f ratmex/pom.xml -P unitTest` | Run unit tests, results are in `ratmex/target/surefire-reports` directory.
`mvn clean test -f ratmex/pom.xml -P concurrencyTest` | Run concurrency tests (takes noticeable time).
`mvn clean install -f ratmex/pom.xml -P doc` | Build RatMeX, generate Javadocs, install these artifacts to local Maven repository. Consider modifying value of `Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE` to true before building production-ready artifacts.
`mvn clean test -f performance-tests/pom.xml -P performanceTest -Dstincmale.ratmex.performance.dryRun=false` | Run performance tests (takes a lot of time), results are in `performance-tests/target/ratmex-performance`. Consider using `-Dstincmale.ratmex.performance.dryRun=true` for dry runs. Take a look at `JmhOptions` to see/modify settings for performance tests. Note that this command also tries to generate result charts if run in headfull JRE (i.e. with UI capabilities).
`mvn test -f performance-tests/pom.xml -P unitTest -Dtest=ChartsGenerator` | Generate charts from results located in `performance-tests/target/ratmex-performance` that were obtained from running performance tests previously, e.g. in headless JRE (i.e. without UI capabilities).
`mvn clean install -f root.xml; mvn clean install -f version.xml; mvn clean install -f build.xml; mvn clean test -f ratmex/pom.xml -P unitTest; mvn clean test -f ratmex/pom.xml -P concurrencyTest; mvn clean install -f ratmex/pom.xml -P doc; mvn clean test -f performance-tests/pom.xml -P performanceTest -Dstincmale.ratmex.performance.dryRun=true; mvn test -f performance-tests/pom.xml -P unitTest -Dtest=ChartsGenerator` | A single combined command doing all the aforementioned in order.
