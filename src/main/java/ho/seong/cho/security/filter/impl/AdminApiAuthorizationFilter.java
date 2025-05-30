package ho.seong.cho.security.filter.impl;

import ho.seong.cho.security.annotation.AdminApi;
import ho.seong.cho.security.data.AdminAuditEvent;
import ho.seong.cho.security.data.CustomAuditorAware;
import ho.seong.cho.security.filter.AbstractMySecurityFilter;
import ho.seong.cho.security.userdetails.MyUserDetails;
import ho.seong.cho.web.resolver.HandlerMethodAnnotationResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@RequiredArgsConstructor
public class AdminApiAuthorizationFilter extends AbstractMySecurityFilter {

  private final HandlerMethodAnnotationResolver annotationResolver;
  private final CustomAuditorAware auditorAware;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (this.shouldNotFilter(request)) {
      this.proceed(request, response, filterChain);
      return;
    }

    Optional<AdminApi> optionalAdminApi = this.annotationResolver.find(request, AdminApi.class);
    final boolean isAdminApi = optionalAdminApi.isPresent();
    final boolean isAuthorizedRequest =
        !isAdminApi || this.isAuthorizedAdmin(optionalAdminApi.get());

    if (!isAuthorizedRequest) {
      this.handleUnauthorizedRequest(request, response, optionalAdminApi.get());
      return;
    }

    try {
      this.proceed(request, response, filterChain);
    } finally {
      if (isAdminApi && optionalAdminApi.get().audit()) {
        this.publishAuditEvent(request, response, optionalAdminApi.get(), true);
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return this.isPublicApiAuthorized(request);
  }

  private boolean isAuthorizedAdmin(AdminApi adminApi) {
    return Optional.of(SecurityContextHolder.getContext())
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .map(authentication -> this.checkAdminPrivileges(authentication, adminApi))
        .orElse(false);
  }

  private boolean checkAdminPrivileges(Authentication authentication, AdminApi adminApi) {
    if (authentication.getPrincipal() instanceof MyUserDetails userDetails) {
      return adminApi.superAdminOnly() ? userDetails.isSuperAdmin() : userDetails.isAdmin();
    }
    return false;
  }

  private void handleUnauthorizedRequest(
      HttpServletRequest request, HttpServletResponse response, AdminApi adminApi)
      throws IOException {
    if (adminApi.audit()) {
      this.publishAuditEvent(request, response, adminApi, false);
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  private void publishAuditEvent(
      HttpServletRequest request,
      HttpServletResponse response,
      AdminApi adminApi,
      boolean isSuccessful) {
    final var username = this.auditorAware.getCurrentAuditorOrAnonymous();
    final var auditEvent =
        AdminAuditEvent.builder()
            .email(username)
            .superAdminOnly(adminApi.superAdminOnly())
            .method(request.getMethod())
            .requestUri(request.getRequestURI())
            .httpStatus(response.getStatus())
            .isSuccessful(isSuccessful)
            .remoteAddress(request.getRemoteAddr())
            .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
            .timestamp(LocalDateTime.now())
            .build();
    this.eventPublisher.publishEvent(auditEvent);
  }
}
