package ho.seong.cho.security.data;

import ho.seong.cho.entity.User;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.userdetails.UserDetails;

/** {@link AuditorAware}를 확장해 현재 인증된 사용자에 대한 Auditing 정보를 제공하는 인터페이스 */
public interface CustomAuditorAware extends AuditorAware<String> {

  String ANONYMOUS_USER = "anonymousUser";

  /**
   * 현재 사용자 정보를 가져온다.
   *
   * @return 현재 사용자 정보, 인증되지 않은 경우 {@link Optional#empty()}
   */
  Optional<User> getCurrentAuditorUser();

  /**
   * 현재 사용자 정보를 가져온다. <br>
   * 인증 정보를 찾을 수 없거나, 인증되지 않은 경우 {@link #ANONYMOUS_USER}를 반환한다.
   *
   * @return 현재 사용자의 이름, 인증되지 않은 경우 {@link #ANONYMOUS_USER}
   * @implNote {@link UserDetails#getUsername()}으로 사용자 이름을 가져온다.
   */
  default String getCurrentAuditorOrAnonymous() {
    return this.getCurrentAuditor().orElse(ANONYMOUS_USER);
  }
}
