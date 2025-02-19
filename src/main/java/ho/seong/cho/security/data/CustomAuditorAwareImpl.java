package ho.seong.cho.security.data;

import ho.seong.cho.security.userdetails.MyUserDetails;
import ho.seong.cho.users.User;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Auditing을 위한 현재 사용자 정보를 제공하는 클래스 */
@Component
public class CustomAuditorAwareImpl implements CustomAuditorAware {

  /**
   * Spring Security의 {@link SecurityContextHolder}를 사용하여 현재 사용자 정보를 가져온다. <br>
   * 인증 정보가 없거나, 인증되지 않은 경우 Optional.empty()를 반환한다. <br>
   * 현재 컨텍스트에서 인증 정보를 가져오므로, 컨텍스트가 전파되지 않는 작업<i>(e.g. @Async)</i>에서는 사용이 불가능할 수 있음!
   *
   * @return 현재 사용자 정보
   */
  @Override
  public Optional<String> getCurrentAuditor() {
    return this.getCurrentUserDetails().map(MyUserDetails::getUsername);
  }

  @Override
  public Optional<User> getCurrentAuditorUser() {
    return this.getCurrentUserDetails().map(MyUserDetails::getUser);
  }

  private boolean isNotAnonymous(Authentication authentication) {
    return !ANONYMOUS_USER.equalsIgnoreCase(authentication.getName());
  }

  private Optional<MyUserDetails> getCurrentUserDetails() {
    return Optional.of(SecurityContextHolder.getContext())
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .filter(this::isNotAnonymous)
        .map(Authentication::getPrincipal)
        .map(MyUserDetails.class::cast);
  }
}
