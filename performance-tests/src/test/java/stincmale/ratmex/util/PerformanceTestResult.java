package stincmale.ratmex.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.json.Json;
import javax.json.JsonReader;
import static java.nio.file.StandardOpenOption.READ;

public final class PerformanceTestResult extends AbstractPerformanceTestResult {
  public PerformanceTestResult(final String testName, final Class<?> testClass) {
    super(testName, testClass);
  }

  /**
   * @return {@code this}.
   */
  public final PerformanceTestResult load() {
    try (final JsonReader jsonReader = Json.createReader(new InputStreamReader(Files.newInputStream(getPath(), READ), StandardCharsets.UTF_8))) {
      System.out.println(jsonReader.read()
          .toString());
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }
}