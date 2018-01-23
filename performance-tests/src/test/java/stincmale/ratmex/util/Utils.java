package stincmale.ratmex.util;

import java.awt.GraphicsEnvironment;
import java.util.Locale;

public final class Utils {
  public static final String format(final String format, final Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  public static final boolean isHeadless() {
    return GraphicsEnvironment.isHeadless() ||
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .isHeadlessInstance();
  }

  private Utils() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}