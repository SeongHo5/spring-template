package ho.seong.cho.utils;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ValidationUtils {

  private ValidationUtils() {}

  /**
   * 입력된 값이 주어진 조건에 맞는지 검사하고, true일 경우 예외를 던진다.
   *
   * @param value 검사할 값 (null인 경우 검사를 생략)
   * @param predicate 검사할 조건을 정의하는 Predicate(true로 평가되면 예외 발생)
   * @param exceptionSupplier 조건에 맞지 않을 경우 발생시킬 예외를 제공하는 Supplier
   * @param <T> 검사할 값의 타입
   * @throws RuntimeException 조건에 맞지 않을 경우 (Supplier가 제공하는 예외)
   */
  public static <T> void throwIf(
      T value, Predicate<T> predicate, Supplier<? extends RuntimeException> exceptionSupplier) {
    if (value != null && predicate.test(value)) {
      throw exceptionSupplier.get();
    }
  }

  /**
   * 하나의 입력 값을 여러 조건에 대해 검사하고, true일 경우 예외를 던진다.
   *
   * @param value 검사할 값 (null인 경우 검사를 생략)
   * @param predicates 검사할 조건을 정의하는 Predicate List
   * @param exceptionSuppliers 조건에 맞지 않을 경우 발생시킬 예외를 제공하는 Supplier List
   * @param <T> 검사할 값의 타입
   * @throws IllegalArgumentException 검사 조건과 예외 Supplier의 수가 일치하지 않을 경우
   * @apiNote {@link #throwIf(Object, Predicate, Supplier)} 메서드에 처리를 위임합니다.
   */
  public static <T> void throwIfAny(
      T value,
      List<Predicate<T>> predicates,
      List<Supplier<? extends RuntimeException>> exceptionSuppliers) {
    if (predicates.size() != exceptionSuppliers.size()) {
      throw new IllegalArgumentException("Size of predicates and exceptionSuppliers MUST be same!");
    }
    for (int i = 0; i < predicates.size(); i++) {
      throwIf(value, predicates.get(i), exceptionSuppliers.get(i));
    }
  }

  /**
   * 주어진 값 중에 null인 값이 하나라도 존재하는지 검사한다.
   *
   * @param objects 검사할 값들
   * @return 하나라도 null인 값이 존재할 경우 true
   */
  public static boolean isAnyNull(Object... objects) {
    return Stream.of(objects).anyMatch(Objects::isNull);
  }
}
