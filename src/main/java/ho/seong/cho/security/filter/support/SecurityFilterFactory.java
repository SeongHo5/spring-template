package ho.seong.cho.security.filter.support;

import ho.seong.cho.security.data.CustomAuditorAware;
import ho.seong.cho.security.filter.impl.AdminApiAuthorizationFilter;
import ho.seong.cho.security.filter.impl.JwtAuthenticationFilter;
import ho.seong.cho.security.filter.impl.PublicApiAccessControlFilter;
import ho.seong.cho.security.filter.impl.RateLimitFilter;
import ho.seong.cho.security.filter.support.impl.RedisTokenBucketRateLimiter;
import ho.seong.cho.web.resolver.HandlerMethodAnnotationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityFilterFactory {

  private final HandlerMethodAnnotationResolver annotationResolver;
  private final CustomAuditorAware auditorAware;
  private final ApplicationEventPublisher eventPublisher;
  private final AuthenticationProvider authenticationProvider;
  private final RedisTokenBucketRateLimiter rateLimiter;

  public PublicApiAccessControlFilter publicAccess() {
    return new PublicApiAccessControlFilter("apiKey", this.annotationResolver);
  }

  public JwtAuthenticationFilter jwtAuth() {
    return new JwtAuthenticationFilter(this.authenticationProvider);
  }

  public AdminApiAuthorizationFilter adminAuth() {
    return new AdminApiAuthorizationFilter(
        this.annotationResolver, this.auditorAware, this.eventPublisher);
  }

  public RateLimitFilter rateLimit() {
    return new RateLimitFilter(this.annotationResolver, this.rateLimiter);
  }
}
