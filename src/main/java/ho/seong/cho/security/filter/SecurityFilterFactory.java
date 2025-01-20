package ho.seong.cho.security.filter;

import ho.seong.cho.entity.CustomAuditorAware;
import ho.seong.cho.jwt.JwtProvider;
import ho.seong.cho.security.MyUserDetailsService;
import ho.seong.cho.security.filter.impl.AdminApiAuthorizationFilter;
import ho.seong.cho.security.filter.impl.JwtAuthenticationFilter;
import ho.seong.cho.security.filter.impl.PublicApiAccessControlFilter;
import ho.seong.cho.web.resolver.HandlerMethodAnnotationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityFilterFactory {

  private final HandlerMethodAnnotationResolver annotationResolver;
  private final CustomAuditorAware auditorAware;
  private final ApplicationEventPublisher eventPublisher;
  private final MyUserDetailsService userDetailsService;
  private final JwtProvider jwtProvider;

  public PublicApiAccessControlFilter publicAccess() {
    return new PublicApiAccessControlFilter("apiKey", this.annotationResolver);
  }

  public JwtAuthenticationFilter jwtAuth() {
    return new JwtAuthenticationFilter(this.jwtProvider, this.userDetailsService);
  }

  public AdminApiAuthorizationFilter adminAuth() {
    return new AdminApiAuthorizationFilter(
        this.annotationResolver, this.auditorAware, this.eventPublisher);
  }
}
