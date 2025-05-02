package ho.seong.cho.jwt.impl;

import static ho.seong.cho.jwt.impl.JwtProperties.BEARER_PREFIX;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public final class JwtUtils {

  private JwtUtils() {}

  /**
   * {@link HttpServletRequest}의 Authorization Header에서 토큰을 추출한다.
   *
   * @param request HTTP 요청
   * @return 추출한 토큰 또는 Bearer 형식이 아니거나 토큰이 없는 경우, {@code null}
   */
  @Nullable public static String resolve(HttpServletRequest request) {
    final var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }

    return authorization.substring(BEARER_PREFIX.length());
  }
}
