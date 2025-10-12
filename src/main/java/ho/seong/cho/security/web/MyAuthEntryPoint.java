package ho.seong.cho.security.web;

import ho.seong.cho.utils.ClassUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@Slf4j
public class MyAuthEntryPoint implements AuthenticationEntryPoint {
  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    if (log.isWarnEnabled()) {
      Throwable cause = ex.getCause();
      if (cause != null) {
        log.warn(
            "UNAUTHORIZED uri='{}'; {}: {} Caused by: {}: {}",
            request.getRequestURI(),
            ClassUtils.getSimpleName(ex),
            ex.getMessage(),
            ClassUtils.getSimpleName(cause),
            cause.getMessage());
      } else {
        log.warn(
            "UNAUTHORIZED uri='{}'; {}: {}",
            request.getRequestURI(),
            ClassUtils.getSimpleName(ex),
            ex.getMessage());
      }
    }
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }
}
