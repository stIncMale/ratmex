package stincmale.ratmex.util;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;

public final class PerformanceTestResult {
  private final String testName;
  private final Class<?> testClass;
  private final Collection<RunResult> runResults;

  public PerformanceTestResult(final String testName, final Class<?> testClass, final Collection<? extends RunResult> runResults) {
    this.testName = testName;
    this.testClass = testClass;
    this.runResults = Collections.unmodifiableCollection(runResults);
  }

  public final void export() {
    final Path resultPath = getResultPath(testClass);
    try (final PrintStream printStream = new PrintStream(
        Files.newOutputStream(resultPath.resolve(testName + ".json"), CREATE, WRITE, TRUNCATE_EXISTING),
        false,
        StandardCharsets.UTF_8.name())) {
      final ResultFormat resultFormat = ResultFormatFactory.getInstance(ResultFormatType.JSON, printStream);
      resultFormat.writeOut(runResults);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Path getResultPath(final Class<?> klass) {
    final Path basePath = getBasePath(klass);
    final Path result = getBasePath(klass).resolve("ratmex-performance");
    return ensureDirectoryPath(result);
  }

  private static final Path getBasePath(final Class<?> klass) {
    final String packageName = klass.getPackageName();
    final long numberOfHopsToBaseDirectory = 2 + packageName.codePoints()
        .filter(codePoint -> codePoint == '.')
        .count();
    final Path pathToClass = getPathToClass(klass).normalize();
    Path result = null;
    for (long i = 0; i < numberOfHopsToBaseDirectory; i++) {
      result = pathToClass.getParent();
    }
    assert result != null;
    return result;
  }

  private static final Path getPathToClass(final Class<?> klass) {
    final URI uri;
    try {
      uri = klass.getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .toURI();
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return Paths.get(uri);
  }

  private static final Path ensureDirectoryPath(final Path path) {
    try {
      Files.createDirectories(path, asFileAttribute(EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return path;
  }
}