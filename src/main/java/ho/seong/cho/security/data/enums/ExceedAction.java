package ho.seong.cho.security.data.enums;

import ho.seong.cho.security.annotation.RateLimit;

/** {@link RateLimit} 기반의 요청 제한 값을 초과했을 때 취할 동작 */
public enum ExceedAction {
  /** 요청을 차단한다. */
  BLOCK,
  /** 요청 처리 속도를 제한한다. */
  THROTTLE,
  /** (차단 없이) 로그만 남긴다. */
  ONLY_LOG
}
