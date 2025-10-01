package ho.seong.cho.utils;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtils {

  StreamUtils() {}

  public static <T> Optional<T> findFirst(Iterable<T> iterable, Predicate<T> predicate) {
    return StreamSupport.stream(iterable.spliterator(), false).filter(predicate).findFirst();
  }

  public static <T> Optional<T> findFirst(T[] tArray, Predicate<T> predicate) {
    return Stream.of(tArray).filter(predicate).findFirst();
  }
}
