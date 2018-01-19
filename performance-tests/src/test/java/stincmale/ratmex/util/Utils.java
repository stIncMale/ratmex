package stincmale.ratmex.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.Locale;

public final class Utils {
  public static final String format(final String format, final Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  public static final boolean isHeadless() {
    boolean result;
    if (GraphicsEnvironment.isHeadless() ||
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .isHeadlessInstance()) {
      result = true;
    } else {
      try {
        final GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getScreenDevices();
        result = screenDevices.length == 0;
      } catch (final HeadlessException e) {
        result = true;
      }
    }
    return result;
  }

  private Utils() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}