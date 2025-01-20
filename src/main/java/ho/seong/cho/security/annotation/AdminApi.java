package ho.seong.cho.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminApi {

  /** 이 API를 최고 관리자만 사용할 수 있도록 설정한다. */
  boolean superAdminOnly() default false;

  /** 이 API 요청 시 감사 로그(logback & DB)를 남길지 여부를 설정한다. */
  boolean audit() default false;
}
