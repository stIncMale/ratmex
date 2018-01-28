package stincmale.ratmex.performance.util;

import java.util.Arrays;
import java.util.Collection;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import stincmale.ratmex.doc.NotThreadSafe;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@NotThreadSafe
public final class JmhOptions {
  public static final boolean DRY_RUN = false;
  private static final boolean JAVA_SERVER = true;
  private static final boolean JAVA_ASSERTIONS = DRY_RUN;
  public static final Collection<Integer> numbersOfThreads = DRY_RUN ? Arrays.asList(1, 4) : Arrays.asList(1, 2, 4, 8, 16);

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
          .warmupTime(milliseconds(100))
          .warmupIterations(1)
          .measurementTime(milliseconds(100))
          .measurementIterations(1);
    } else {
      result.forks(2)
          .warmupTime(milliseconds(300))
          .warmupIterations(10)
          .measurementTime(milliseconds(500))
          .measurementIterations(5);
    }
    return result;
  }

  private JmhOptions() {
    throw new UnsupportedOperationException();
  }
}