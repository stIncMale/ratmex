package stincmale.ratmex.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import org.openjdk.jmh.annotations.Mode;
import static java.nio.file.StandardOpenOption.READ;

public final class PerformanceTestResult extends AbstractPerformanceTestResult {
  public PerformanceTestResult(final String testId, final Class<?> testClass) {
    super(testId, testClass);
  }

  /**
   * @return {@code this}.
   */
  public final PerformanceTestResult load() {
    try (final JsonReader jsonReader = Json.createReader(new InputStreamReader(Files.newInputStream(getPath(), READ), StandardCharsets.UTF_8))) {
      jsonReader.readArray()
          .stream()
          .map(JsonValue::asJsonObject)
          .map(jsonObj -> new BenchmarkResult(getTestId(), jsonObj))
          .forEach(System.out::println);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public static final class BenchmarkResult {
    public final String testId;
    public final String benchmark;
    public final Mode mode;
    public final int numberOfThreads;
    public final double score;
    public final double error;
    public final String unit;

    private BenchmarkResult(
        final String testId,
        final String benchmark,
        final Mode mode,
        final int numberOfThreads,
        final double score,
        final double error,
        final String unit) {
      this.testId = testId;
      this.benchmark = benchmark;
      this.mode = mode;
      this.numberOfThreads = numberOfThreads;
      this.score = score;
      this.error = error;
      this.unit = unit;
    }

    private BenchmarkResult(final String testId, final JsonObject jsonObj) {
      this(
          testId,
          getName(jsonObj),
          Mode.deepValueOf(jsonObj.getString("mode")),
          jsonObj.getInt("threads"),
          getDouble(jsonObj.getJsonObject("primaryMetric")
              .get("score")),
          getDouble(jsonObj.getJsonObject("primaryMetric")
              .get("scoreError")),
          jsonObj.getJsonObject("primaryMetric")
              .getString("scoreUnit"));
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() +
          "{testId=" + testId +
          ", benchmark=" + benchmark +
          ", mode=" + mode +
          ", numberOfThreads=" + numberOfThreads +
          ", score=" + score +
          ", error=" + error +
          ", unit=" + unit +
          '}';
    }

    private static final String getName(final JsonObject jsonObj) {
      final String fullName = jsonObj.getString("benchmark");
      return fullName.substring(fullName.lastIndexOf('.') + 1);
    }

    private static final double getDouble(final JsonValue jsonVal) {
      final double result;
      if (jsonVal.getValueType() == ValueType.NUMBER) {
        result = ((JsonNumber)jsonVal).doubleValue();
      } else if (jsonVal.getValueType() == ValueType.NULL) {
        result = Double.NaN;
      } else {//ValueType.STRING
        result = Double.parseDouble(((JsonString)jsonVal).getString());
      }
      return result;
    }
  }
}