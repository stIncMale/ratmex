package stincmale.ratmex.performance.util;

import java.util.Arrays;
import java.util.Collection;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import stincmale.ratmex.doc.NotThreadSafe;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@NotThreadSafe
public final class JmhOptions {
  public static final boolean DRY_RUN = true;
  private static final boolean JAVA_SERVER = true;
  private static final boolean JAVA_ASSERTIONS = false;
  public static final Collection<Integer> numbersOfThreads = Arrays.asList(1, 2, 4, 8, 16, 32);

  public static final ChainedOptionsBuilder includingClass(final Class<?> klass) {
    return get().include(klass.getName() + ".*");
  }

  public static final ChainedOptionsBuilder get() {
    final ChainedOptionsBuilder result = new OptionsBuilder()
        .jvmArgsAppend(JAVA_SERVER ? "-server" : "-client")
        .jvmArgsAppend(JAVA_ASSERTIONS ? "-enableassertions" : "-disableassertions")
        .shouldDoGC(true)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1)
        .timeout(milliseconds(5_000));
    if (DRY_RUN) {
      result.forks(1)
          .warmupTime(milliseconds(1))
          .warmupIterations(0)
          .measurementTime(milliseconds(1))
          .measurementIterations(1);
    } else {
      result.forks(3)
          .warmupTime(milliseconds(200))
          .warmupIterations(7)
          .measurementTime(milliseconds(200))
          .measurementIterations(5);
    }
    return result;
  }

  private JmhOptions() {
    throw new UnsupportedOperationException();
  }
}