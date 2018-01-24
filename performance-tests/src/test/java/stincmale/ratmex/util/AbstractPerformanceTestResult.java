package stincmale.ratmex.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;

class AbstractPerformanceTestResult {
  private final String testId;
  private final Class<?> testClass;
  private final Path path;

  protected AbstractPerformanceTestResult(final String testId, final Class<?> testClass) {
    this.testId = testId;
    this.testClass = testClass;
    path = getDirectoryPath(testClass).resolve(testId + ".json");
  }

  protected final Path getPath() {
    return path;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{testId='" + testId +
        ", testClass=" + testClass +
        ", path=" + path +
        '}';
  }

  private static final Path getDirectoryPath(final Class<?> klass) {
    final Path basePath = getBasePath(klass);
    final Path result = getBasePath(klass).resolve("ratmex-performance");
    return ensureDirectoryPath(result);
  }

  private static final Path getBasePath(final Class<?> klass) {
    final String packageName = klass.getPackage()
        .getName();
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