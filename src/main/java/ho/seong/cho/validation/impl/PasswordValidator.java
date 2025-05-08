package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.Nickname;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator extends CustomConstraintValidator<Nickname, String> {

  public static final Pattern PASSWORD_PATTERN = Pattern.compile("^[가-힣]{1,12}$");

  @Override
  public void initialize(Nickname constraintAnnotation) {
    this.isRequired = constraintAnnotation.required();
  }

  @Override
  public boolean isValidInternal(String value, ConstraintValidatorContext context) {
    return value != null && PASSWORD_PATTERN.matcher(value).matches();
  }
}
