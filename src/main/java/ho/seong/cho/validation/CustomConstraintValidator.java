package ho.seong.cho.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

public abstract class CustomConstraintValidator<A extends Annotation, T>
    implements ConstraintValidator<A, T> {

  /**
   * 제약 조건 위반 시 출력되는 메세지를 기본 메세지 대신 사용자 정의 메세지로 설정합니다.
   *
   * @param context ConstraintValidatorContext
   * @param message 사용자 정의 메세지
   * @param fieldName 필드 이름
   */
  protected void setCustomViolationMessage(
      ConstraintValidatorContext context, final String message, final String fieldName) {
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(fieldName)
        .addConstraintViolation();
  }
}
