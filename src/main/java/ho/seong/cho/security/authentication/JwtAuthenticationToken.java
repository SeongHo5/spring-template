package ho.seong.cho.security.authentication;

import ho.seong.cho.security.userdetails.MyUserDetails;
import io.jsonwebtoken.lang.Assert;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

/** JWT 기반 인증을 위한 {@link Authentication} 구현체 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  @Serial private static final long serialVersionUID = 2025010101L;

  private final MyUserDetails principal;

  private final String credentials;

  /**
   * JWT 기반 인증 요청 객체를 생성한다. <br>
   * 이 생성자는 <strong>인증되지 않은</strong> 상태를 나타낸다.
   *
   * @param token 인증 요청에 사용할 JWT 토큰
   */
  public JwtAuthenticationToken(final String token) {
    super(null);
    this.principal = null;
    this.credentials = token;
    super.setAuthenticated(false);
  }

  /**
   * 인증이 <strong>완료된</strong> {@link JwtAuthenticationToken} 객체를 생성한다.
   *
   * @param principal 인증된 사용자의 {@link MyUserDetails} 정보
   */
  private JwtAuthenticationToken(MyUserDetails principal) {
    super(principal.getAuthorities());
    this.principal = principal;
    this.credentials = null;
    super.setAuthenticated(true);
  }

  /**
   * 인증된 사용자 정보를 가지고 있는 {@link JwtAuthenticationToken} 객체를 생성한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @return 인증된 사용자 정보를 가지고 있는 {@link JwtAuthenticationToken} 객체
   * @throws IllegalArgumentException 인증된 사용자 정보가 {@code null}인 경우
   */
  public static JwtAuthenticationToken authenticated(@NotNull MyUserDetails userDetails) {
    Assert.notNull(userDetails, "'userDetails' must not be null.");
    return new JwtAuthenticationToken(userDetails);
  }

  /**
   * 인증 주체(Principal)를 반환한다.
   *
   * <p>인증 전: {@code null}
   *
   * <p>인증 후: {@link MyUserDetails}
   *
   * @return 인증 주체(Principal)
   */
  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  /**
   * 인증 정보(Credentials)를 반환한다.
   *
   * <p>인증 전: JWT 토큰
   *
   * <p>인증 후: {@code null}
   *
   * @return 인증 정보(Credentials)
   */
  @Override
  public Object getCredentials() {
    return this.credentials;
  }

  /**
   * 인증 상태를 설정한다. <br>
   * 인증 상태를 {@code true}로 설정하려면 {@link #principal}가 {@code null}이 아니어야 한다. <br>
   *
   * @param isAuthenticated 인증 상태
   * @throws IllegalArgumentException 유효한 사용자 정보가 없는데 인증 상태를 {@code true}로 설정하려는 경우
   */
  @Override
  public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {
    if (isAuthenticated && this.principal == null) {
      throw new IllegalArgumentException(
          "Cannot set authenticated without valid user details. Use constructor instead.");
    }
    super.setAuthenticated(isAuthenticated);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof JwtAuthenticationToken that) {
      return Objects.equals(this.credentials, that.credentials);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.credentials);
  }
}
