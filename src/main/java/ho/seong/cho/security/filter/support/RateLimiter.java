package ho.seong.cho.security.filter.support;

import ho.seong.cho.security.annotation.RateLimit;

/** API 요청 제한을 위한 RateLimiter 인터페이스 */
public interface RateLimiter {

  /**
   * 현재 요청이 제한을 초과했는지 확인한다.
   *
   * @param key 요청 식별자
   * @param rateLimit 요청 제한 정보
   * @return 제한을 초과했으면 true, 아니면 false
   */
  boolean isExceeded(String key, RateLimit rateLimit);

  /**
   * 요청을 처리하고 토큰을 차감한다.
   *
   * @param key 요청 식별자
   * @param rateLimit 요청 제한 정보
   * @return 요청이 허용되었는지 여부
   */
  boolean tryConsume(String key, RateLimit rateLimit);

  /**
   * 현재 남은 허용량(토큰, 요청 횟수 등)을 반환한다.
   *
   * @param key 요청 식별자
   * @return 현재 허용된 요청 수 또는 토큰 개수
   */
  int getRemainingTokens(String key);

  /**
   * 현재 허용량을 초기화한다.
   *
   * @param key 요청 식별자
   * @param rateLimit 요청 제한 정보
   */
  void reset(String key, RateLimit rateLimit);
}
