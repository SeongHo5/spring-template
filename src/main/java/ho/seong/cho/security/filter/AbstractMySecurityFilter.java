package ho.seong.cho.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public abstract class AbstractMySecurityFilter extends OncePerRequestFilter {

  /** Public API로 인증된 요청임을 나타내는 {@link HttpServletRequest} 속성 이름 */
  protected static final String PUBLIC_API_AUTHORIZED = "SECURITY_PUBLIC_API_AUTHORIZED";

  protected void proceed(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);
  }

  /**
   * 현재 요청이 Public API로 인증된 요청인지 확인한다.
   *
   * @param request 요청
   * @return Public API로 인증된 요청인 경우 true, 그렇지 않은 경우 false
   */
  protected boolean isPublicApiAuthorized(HttpServletRequest request) {
    return Boolean.TRUE.equals(request.getAttribute(PUBLIC_API_AUTHORIZED));
  }
}
