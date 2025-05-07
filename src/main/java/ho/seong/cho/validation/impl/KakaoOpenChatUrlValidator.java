package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.KakaoOpenChatUrl;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class KakaoOpenChatUrlValidator extends CustomConstraintValidator<KakaoOpenChatUrl, String> {

  private static final Pattern KAKAO_OPEN_CHAT_URL_PATTERN =
      Pattern.compile("^https://open.kakao.com/o/[a-zA-Z0-9]{6,}$");

  @Override
  public void initialize(KakaoOpenChatUrl constraintAnnotation) {
    this.isRequired = constraintAnnotation.required();
  }

  @Override
  protected boolean isValidInternal(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return false;
    }
    return KAKAO_OPEN_CHAT_URL_PATTERN.matcher(value).matches();
  }
}
