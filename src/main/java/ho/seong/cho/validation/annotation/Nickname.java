package ho.seong.cho.validation.annotation;

import ho.seong.cho.validation.impl.NicknameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NicknameValidator.class)
public @interface Nickname {

  /**
   * 필드 값의 필수 여부를 지정한다. {@code true}이면 해당 값이 null이어도, 검증을 통과한다.
   *
   * @return 필수 여부
   */
  boolean required() default true;

  String message() default "{ho.seong.cho.validator.constraints.Nickname.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
