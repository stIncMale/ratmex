package stincmale.ratmex.util;

import javax.annotation.concurrent.NotThreadSafe;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@NotThreadSafe
public final class JmhOptions {
  private static final boolean DRY_RUN = false;
  private static final boolean JAVA_SERVER = true;
  private static final boolean JAVA_ASSERTIONS = false;

  public static final ChainedOptionsBuilder includingClass(final Class<?> klass) {
    return get().include(klass.getName() + ".*");
  }

  public static final ChainedOptionsBuilder get() {
    final ChainedOptionsBuilder result = new OptionsBuilder()
        .jvmArgsAppend(JAVA_SERVER ? "-server" : "-client")
        .jvmArgsAppend(JAVA_ASSERTIONS ? "-enableassertions" : "-disableassertions")
        .shouldDoGC(false)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1);
    if (DRY_RUN) {
      result.forks(1)
          .timeout(milliseconds(1_000))
          .warmupTime(milliseconds(1))
          .warmupIterations(0)
          .measurementTime(milliseconds(1))
          .measurementIterations(1);
    } else {
      result.forks(3)
          .timeout(milliseconds(10_000))
          .warmupTime(milliseconds(200))
          .warmupIterations(5)
          .measurementTime(milliseconds(200))
          .measurementIterations(5);
    }
    return result;
  }

  private JmhOptions() {
    throw new UnsupportedOperationException();
  }
}