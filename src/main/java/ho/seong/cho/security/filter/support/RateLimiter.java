package ho.seong.cho.security.filter.support;

import ho.seong.cho.security.annotation.RateLimit;

/** API 요청 제한을 위한 RateLimiter 인터페이스 */
public interface RateLimiter {

  /**
   * 요청 횟수를 1 증가시킨다.
   *
   * @param key 요청 식별자
   */
  void increment(String key);

  /**
   * 요청 횟수를 초기화한다.
   *
   * @param key 요청 식별자
   * @param rateLimit 요청 제한 정보
   * @return 현재 요청이 RateLimit 제한을 초과했는지 여부
   */
  boolean isExceeded(String key, RateLimit rateLimit);

  /**
   * 요청 횟수를 1 증가시킨 후 초과 여부를 반환한다. <br>
   * 요청 카운트를 증가시키고, 바로 초과 여부를 확인할 때 사용 가능한 단축 메서드
   *
   * @param key 요청 식별자
   * @param rateLimit 요청 제한 정보
   * @return 현재 요청이 RateLimit 제한을 초과했는지 여부
   */
  default boolean incrementAndCheckExceeded(String key, RateLimit rateLimit) {
    this.increment(key);
    return this.isExceeded(key, rateLimit);
  }
}
