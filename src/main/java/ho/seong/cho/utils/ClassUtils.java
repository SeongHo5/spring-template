package ho.seong.cho.utils;

public final class ClassUtils {

  private ClassUtils() {}

  public static String getSimpleName(final Object object) {
    return getSimpleName(object, "");
  }

  public static String getSimpleName(final Object object, final String valueIfNull) {
    return object == null ? valueIfNull : object.getClass().getSimpleName();
  }
}
