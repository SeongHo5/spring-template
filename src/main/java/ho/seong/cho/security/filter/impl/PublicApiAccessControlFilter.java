package ho.seong.cho.security.filter.impl;

import ho.seong.cho.security.annotation.PublicApi;
import ho.seong.cho.security.filter.AbstractMySecurityFilter;
import ho.seong.cho.web.resolver.HandlerMethodAnnotationResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PublicApiAccessControlFilter extends AbstractMySecurityFilter {

  private final String publicApiKey;
  private final HandlerMethodAnnotationResolver annotationResolver;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<PublicApi> optionalPublicApi = this.annotationResolver.find(request, PublicApi.class);
    final boolean isPublicApi = optionalPublicApi.isPresent();

    if (isPublicApi && this.isAccessAllowed(request, optionalPublicApi.get())) {
      request.setAttribute(PUBLIC_API_AUTHORIZED, true);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * 요청에 대한 접근 권한을 검증한다. PUBLIC 접근 레벨인 경우 항상 true를 반환하고, PROTECTED 접근 레벨인 경우 API 키를 검증한다.
   *
   * @param request HTTP 요청
   * @param publicApi PublicApi 어노테이션 정보
   * @return 접근 허용 여부
   */
  private boolean isAccessAllowed(HttpServletRequest request, PublicApi publicApi) {
    return switch (publicApi.accessLevel()) {
      case PUBLIC -> true;
      case PROTECTED -> this.validateApiKey(request, publicApi);
    };
  }

  /**
   * API 키의 유효성을 검증한다. REQUIRED: API 키가 존재하고 유효해야 한다. OPTIONAL: API 키가 없거나, 있다면 유효해야 한다. NONE:
   * PROTECTED 접근 레벨과 함께 사용할 수 없음
   *
   * @param request HTTP 요청
   * @param publicApi PublicApi 어노테이션 정보
   * @return API 키 유효성 여부
   * @throws IllegalStateException PROTECTED 접근 레벨에서 NONE 검증이 설정된 경우
   */
  private boolean validateApiKey(HttpServletRequest request, PublicApi publicApi) {
    Optional<String> requestedHeader = this.extractHeader(request, publicApi.keyHeader());
    return switch (publicApi.keyValidation()) {
      case REQUIRED ->
          requestedHeader.isPresent() && this.publicApiKey.equals(requestedHeader.get());
      case OPTIONAL -> requestedHeader.isEmpty() || this.publicApiKey.equals(requestedHeader.get());
      case NONE ->
          throw new IllegalStateException(
              "If access level is PROTECTED, key validation must be REQUIRED or OPTIONAL");
    };
  }

  private Optional<String> extractHeader(HttpServletRequest request, String headerName) {
    return Optional.of(request).map(req -> req.getHeader(headerName));
  }
}
