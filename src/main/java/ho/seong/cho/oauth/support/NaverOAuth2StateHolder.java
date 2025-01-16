package ho.seong.cho.oauth.support;

import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

/** NAVER OAuth2 인증 처리 시 사용되는 상태({@code state}) 값을 관리하는 클래스 */
public final class NaverOAuth2StateHolder {

  private static final ThreadLocal<String> stateHolder = new ThreadLocal<>();

  private NaverOAuth2StateHolder() {}

  /**
   * 상태(state) 값을 저장한다.
   *
   * @param state 상태 값 (never {@code null})
   * @throws IllegalArgumentException 상태 값이 {@code null}인 경우
   */
  public static void set(@NotNull String state) {
    Assert.notNull(state, "State must not be null");
    stateHolder.set(state);
  }

  /**
   * 저장된 상태(state) 값을 가져온다.
   *
   * @return 상태 값
   * @throws IllegalStateException 설정된 상태 값이 없는데 이 메서드가 호출된 경우
   */
  public static String consume() {
    String state = stateHolder.get();
    if (state == null) {
      throw new IllegalStateException("State is not set");
    }

    stateHolder.remove();
    return state;
  }
}
