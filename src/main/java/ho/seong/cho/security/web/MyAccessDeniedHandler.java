package ho.seong.cho.security.web;

import ho.seong.cho.utils.ClassUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@Slf4j
public class MyAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
    if (log.isWarnEnabled()) {
      Throwable cause = ex.getCause();
      if (cause != null) {
        log.warn(
            "FORBIDDEN uri='{}'; {}: {} Caused by: {}: {}",
            request.getRequestURI(),
            ClassUtils.getSimpleName(ex),
            ex.getMessage(),
            ClassUtils.getSimpleName(cause),
            cause.getMessage());
      } else {
        log.warn(
            "FORBIDDEN uri='{}'; {}: {}",
            request.getRequestURI(),
            ClassUtils.getSimpleName(ex),
            ex.getMessage());
      }
    }
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
  }
}
