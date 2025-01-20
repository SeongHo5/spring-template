package ho.seong.cho.security.filter.impl;

import ho.seong.cho.security.annotation.RateLimit;
import ho.seong.cho.security.filter.AbstractMySecurityFilter;
import ho.seong.cho.security.filter.support.RateLimiter;
import ho.seong.cho.web.resolver.HandlerMethodAnnotationResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

@RequiredArgsConstructor
public class RateLimitFilter extends AbstractMySecurityFilter {

  private final HandlerMethodAnnotationResolver annotationResolver;
  private final RateLimiter rateLimiter;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<RateLimit> optionalRateLimit = this.annotationResolver.find(request, RateLimit.class);

    if (optionalRateLimit.isEmpty()) {
      this.proceed(request, response, filterChain);
      return;
    }

    RateLimit rateLimit = optionalRateLimit.get();
    Assert.isTrue(rateLimit.maxRequests() > 0, "maxRequests must be greater than 0");
    Assert.isTrue(rateLimit.duration() > 0, "duration must be greater than 0");

    String key = generateRateLimitKey(request);

    final boolean isRateLimitExceeded = this.rateLimiter.incrementAndCheckExceeded(key, rateLimit);

    if (this.needsRateLimitHandling(isRateLimitExceeded, rateLimit)) {
      this.doHandle(response, rateLimit);
      return;
    }

    this.proceed(request, response, filterChain);
  }

  private String generateRateLimitKey(HttpServletRequest request) {
    throw new UnsupportedOperationException("인증 | 비인증 사용자별 Key 생성 로직 구현 필요");
  }

  private boolean needsRateLimitHandling(final boolean isExceeded, RateLimit rateLimit) {
    return switch (rateLimit.exceedAction()) {
      case BLOCK -> isExceeded;
      case ONLY_LOG -> {
        if (isExceeded) {
          logger.warn("Rate limit exceeded for action: ONLY_LOG (key).");
        }
        yield false;
      }
      case THROTTLE -> {
        if (isExceeded) {
          this.handleThrottle(rateLimit);
        }
        yield false;
      }
    };
  }

  private void doHandle(HttpServletResponse response, RateLimit rateLimit) throws IOException {
    if (rateLimit.includeRetryAfterHeader()) {
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimit.duration()));
    }
    response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  private void handleThrottle(RateLimit rateLimit) {
    try {
      Thread.sleep(rateLimit.duration());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Throttle sleep interrupted", e);
    }
  }
}
