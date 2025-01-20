package ho.seong.cho.security.annotation;

import ho.seong.cho.security.common.AccessLevel;
import ho.seong.cho.security.common.KeyValidation;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicApi {

  /** API 접근 제어 수준을 설정한다. */
  AccessLevel accessLevel() default AccessLevel.PROTECTED;

  /** API Key 검증 여부를 설정한다. */
  KeyValidation keyValidation() default KeyValidation.REQUIRED;

  /** API Key를 검증할 때 사용할 헤더 이름을 설정한다. */
  String keyHeader() default "Locat-Api-Key";
}
