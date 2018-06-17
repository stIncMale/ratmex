/*
 * Copyright 2017-2018 Valiantsin Kavalenka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stincmale.ratmex.performance.util;

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
import static stincmale.ratmex.performance.util.Utils.format;

abstract class AbstractPerformanceTestResult {
  private final String testId;
  private final Path directoryPath;
  private final Path dataFilePath;
  private final Path environmentDescriptionFilePath;

  protected static final String getEnvironmentDescription() {
    final int availableProcessors = Runtime.getRuntime()
        .availableProcessors();
    final String jvm = format("JVM: %s %s", System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
    final String os = format("OS: %s %s %s", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
    return format("%s, %s, processors: %s", jvm, os, availableProcessors);
  }

  protected AbstractPerformanceTestResult(final String testId) {
    this.testId = testId;
    directoryPath = getDirectoryPath(getClass());
    dataFilePath = directoryPath.resolve(testId + ".json");
    environmentDescriptionFilePath = directoryPath.resolve(format("%s-environmentDescription", testId) + ".txt");
  }

  public final String getTestId() {
    return testId;
  }

  protected final Path getDirectoryPath() {
    return directoryPath;
  }

  protected final Path getDataFilePath() {
    return dataFilePath;
  }

  protected final Path getEnvironmentDescriptionFilePath() {
    return environmentDescriptionFilePath;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{testId=" + testId +
        ", directoryPath=" + directoryPath +
        ", dataFilePath=" + dataFilePath +
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