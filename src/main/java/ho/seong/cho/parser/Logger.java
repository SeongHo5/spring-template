package ho.seong.cho.parser;

final class Logger {

  static void info(String message) {
    System.out.println("[INFO] " + message);
  }

  static void warn(String message) {
    System.err.println("[WARN] " + message);
  }

  static void error(String message) {
    System.err.println("[ERROR] " + message);
  }

  static void error(String message, Exception e) {
    System.err.println(
        "[ERROR] " + message + " | " + e.getClass().getSimpleName() + ": " + truncate(e));
  }

  private static String truncate(Throwable t) {
    String m = t.getMessage();
    if (m == null) {
      return "";
    }
    m = m.replace('\n', ' ').replace('\r', ' ');
    return m.length() > 200 ? m.substring(0, 200) + "..." : m;
  }
}
