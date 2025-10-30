package ho.seong.cho.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

public abstract class CustomConstraintValidator<A extends Annotation, T>
    implements ConstraintValidator<A, T> {

  protected boolean required;

  @Override
  public boolean isValid(T value, ConstraintValidatorContext context) {
    if (!this.required && value == null) {
      return true; // 필수가 아니고 값이 null이면 유효하다고 판단
    }
    // 필수이거나 값이 null이 아닌 경우 구현체별 유효성 검사 수행
    return value != null && this.isValidInternal(value, context);
  }

  protected abstract boolean isValidInternal(T value, ConstraintValidatorContext context);

  /**
   * 제약 조건 위반 시 출력되는 메세지를 기본 메세지 대신 사용자 정의 메세지로 설정합니다.
   *
   * @param context ConstraintValidatorContext
   * @param message 사용자 정의 메세지
   * @param fieldName 필드 이름
   */
  protected void addCustomViolationMessage(
      ConstraintValidatorContext context, final String message, final String fieldName) {
    context.disableDefaultConstraintViolation();
    final var violationBuilder = context.buildConstraintViolationWithTemplate(message);
    if (fieldName != null) {
      violationBuilder.addPropertyNode(fieldName);
    }
    violationBuilder.addConstraintViolation();
  }
}
