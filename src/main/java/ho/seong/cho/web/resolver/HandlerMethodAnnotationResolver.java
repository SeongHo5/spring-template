package ho.seong.cho.web.resolver;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@Slf4j
@RequiredArgsConstructor
public class HandlerMethodAnnotationResolver {

  private static final String RESOLVED_HANDLER_METHOD_ATTRIBUTE =
      HandlerMethodAnnotationResolver.class.getName() + ".RESOLVED_HANDLER_METHOD";

  private final RequestMappingHandlerMapping requestMappingHandlerMapping;

  /**
   * HTTP 요청에서 {@link HandlerMethod}를 찾고, 해당 핸들러 메서드에서 지정된 어노테이션을 찾아 반환한다. <br>
   * 메서드 > 클래스 레벨의 순서로 어노테이션을 찾는다.
   *
   * @param request HTTP 요청
   * @param targetAnnotation 찾을 어노테이션
   * @return 찾은 어노테이션, 없을 경우 {@link Optional#empty()}
   * @param <A> 찾을 어노테이션의 타입
   */
  public <A extends Annotation> Optional<A> find(
      HttpServletRequest request, Class<A> targetAnnotation) {
    return this.resolveHandlerMethod(request)
        .flatMap(handlerMethod -> this.findAnnotation(handlerMethod, targetAnnotation));
  }

  private Optional<HandlerMethod> resolveHandlerMethod(HttpServletRequest request) {
    Object attribute = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

    if (attribute instanceof HandlerMethod handlerMethod) {
      return Optional.of(handlerMethod);
    }

    attribute = request.getAttribute(RESOLVED_HANDLER_METHOD_ATTRIBUTE);

    if (attribute instanceof HandlerMethod handlerMethod) {
      return Optional.of(handlerMethod);
    }

    return this.resolveByHandlerMapping(request);
  }

  private Optional<HandlerMethod> resolveByHandlerMapping(HttpServletRequest request) {
    try {
      HandlerExecutionChain chain = this.requestMappingHandlerMapping.getHandler(request);
      if (chain == null) {
        return Optional.empty();
      }

      Object handler = chain.getHandler();
      if (!(handler instanceof HandlerMethod handlerMethod)) {
        return Optional.empty();
      }

      request.setAttribute(RESOLVED_HANDLER_METHOD_ATTRIBUTE, handlerMethod);
      return Optional.of(handlerMethod);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to resolve handler method. uri={}", request.getRequestURI(), e);
      }
      return Optional.empty();
    }
  }

  private <A extends Annotation> Optional<A> findAnnotation(
      HandlerMethod handlerMethod, Class<A> targetAnnotation) {

    A annotation =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), targetAnnotation);
    if (annotation != null) {
      return Optional.of(annotation);
    }

    return Optional.ofNullable(
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), targetAnnotation));
  }
}
