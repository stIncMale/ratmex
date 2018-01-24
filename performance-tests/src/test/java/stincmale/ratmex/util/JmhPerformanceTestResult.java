package stincmale.ratmex.util;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class JmhPerformanceTestResult extends AbstractPerformanceTestResult {
  private final Collection<RunResult> runResults;

  public JmhPerformanceTestResult(final String testId, final Class<?> testClass, final Collection<? extends RunResult> runResults) {
    super(testId, testClass);
    this.runResults = Collections.unmodifiableCollection(runResults);
  }

  public final void save() {
    try (final PrintStream printStream = new PrintStream(
        Files.newOutputStream(getPath(), CREATE, WRITE, TRUNCATE_EXISTING),
        false,
        StandardCharsets.UTF_8.name())) {
      final ResultFormat resultFormat = ResultFormatFactory.getInstance(ResultFormatType.JSON, printStream);
      resultFormat.writeOut(runResults);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}