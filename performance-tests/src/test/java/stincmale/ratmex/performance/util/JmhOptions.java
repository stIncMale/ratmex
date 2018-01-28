package stincmale.ratmex.performance.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import stincmale.ratmex.doc.NotThreadSafe;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@NotThreadSafe
public final class JmhOptions {
  public static final boolean DRY_RUN = true;
  private static final boolean JAVA_SERVER = true;
  private static final boolean JAVA_ASSERTIONS = DRY_RUN;
  public static final SortedSet<Integer> numbersOfThreads = DRY_RUN
      ? new TreeSet<>(Arrays.asList(1, 4))
      : new TreeSet<>(Arrays.asList(1, 2, 4, 8, 16));

  public static final OptionsBuilder includingClass(final Class<?> klass) {
    final OptionsBuilder result = get();
    result.include(klass.getName() + ".*");
    return result;
  }

  public static final OptionsBuilder get() {
    final OptionsBuilder result = new OptionsBuilder();
    result.jvmArgsAppend(
        JAVA_SERVER ? "-server" : "-client",
        JAVA_ASSERTIONS ? "-enableassertions" : "-disableassertions")
        .shouldDoGC(true)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1)
        .timeout(milliseconds(5_000));
    if (DRY_RUN) {
      result.forks(1)
          .warmupTime(milliseconds(50))
          .warmupIterations(1)
          .measurementTime(milliseconds(50))
          .measurementIterations(1);
    } else {
      result.forks(2)
          .warmupTime(milliseconds(200))
          .warmupIterations(10)
          .measurementTime(milliseconds(200))
          .measurementIterations(6);
    }
    return result;
  }

  public static final OptionsBuilder jvmArgsAppend(final OptionsBuilder ob, final String... append) {
    final Collection<String> jvmArgsAppend = ob.getJvmArgsAppend()
        .orElse(Collections.emptyList());
    final Collection<String> newJvmArgsAppend = new ArrayList<>(jvmArgsAppend);
    newJvmArgsAppend.addAll(Arrays.asList(append));
    ob.jvmArgsAppend(newJvmArgsAppend.toArray(new String[newJvmArgsAppend.size()]));
    return ob;
  }

  private JmhOptions() {
    throw new UnsupportedOperationException();
  }
}