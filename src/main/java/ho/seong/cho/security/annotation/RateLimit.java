package ho.seong.cho.security.annotation;

import ho.seong.cho.security.data.enums.ExceedAction;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/** API 요청 제한을 설정하는 어노테이션 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

  /**
   * 허용되는 최대 요청수 <br>
   * 반드시 1 이상의 값을 가져야 함
   *
   * @return 허용되는 최대 요청수
   * @throws IllegalArgumentException 0 이하의 값을 가질 경우
   */
  int maxRequests();

  /**
   * 제한 시간 간격 <br>
   * 반드시 1 이상의 값을 가져야 함
   *
   * @return 제한 시간 간격
   * @throws IllegalArgumentException 0 이하의 값을 가질 경우
   */
  int duration();

  /**
   * 제한 시간 간격 단위
   *
   * @return 제한 시간 간격 단위
   */
  ChronoUnit durationUnit() default ChronoUnit.MINUTES;

  /**
   * 요청 허용 초과 시 수행할 동작
   *
   * @return 초과 시 처리 방법 / 기본값은 요청을 차단한다.
   */
  ExceedAction exceedAction() default ExceedAction.BLOCK;

  /**
   * 요청이 초과된 경우 {@code Retry-After} 헤더를 반환할지 여부
   *
   * @return {@code Retry-After} 헤더 반환 여부
   */
  boolean includeRetryAfterHeader() default true;
}
