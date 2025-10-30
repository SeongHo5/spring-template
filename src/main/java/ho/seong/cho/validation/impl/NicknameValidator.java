package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.Nickname;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NicknameValidator extends CustomConstraintValidator<Nickname, String> {

  public static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣]{1,12}$");
  public static final Pattern FORBIDDEN_NICKNAME_PATTERN =
      Pattern.compile("관리자|운영자|관리|운영", Pattern.UNICODE_CASE);

  @Override
  public void initialize(Nickname constraintAnnotation) {
    this.required = constraintAnnotation.required();
  }

  @Override
  public boolean isValidInternal(String value, ConstraintValidatorContext context) {
    if (value == null || !NICKNAME_PATTERN.matcher(value).matches()) {
      return false;
    }

    if (FORBIDDEN_NICKNAME_PATTERN.matcher(value).find()) {
      this.addCustomViolationMessage(
          context, "ho.seong.cho.validator.constraints.Nickname.invalid.forbidden", "nickname");
      return false;
    }

    return true;
  }
}
