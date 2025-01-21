package ho.seong.cho.security.authentication;

import ho.seong.cho.users.User;
import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  @Serial private static final long serialVersionUID = 2025010101L;

  @Getter private final String token;

  private final String principal;

  private final String credentials;

  public JwtAuthenticationToken(String token) {
    super(null);
    this.token = token;
    this.principal = null;
    this.credentials = null;
    super.setAuthenticated(false);
  }

  private JwtAuthenticationToken(
      String principal, String credentials, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.token = null;
    this.principal = principal;
    this.credentials = credentials;
    super.setAuthenticated(true);
  }

  public static JwtAuthenticationToken from(User authenticatedUser) {
    final String roleType = authenticatedUser.getRoleType().name();
    return new JwtAuthenticationToken(
        authenticatedUser.getName(),
        roleType,
        Collections.singleton((GrantedAuthority) () -> roleType));
  }

  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  @Override
  public Object getCredentials() {
    return this.credentials;
  }

  @Override
  public void setAuthenticated(final boolean isAuthenticated) {
    if (isAuthenticated && this.token == null) {
      throw new IllegalArgumentException(
          "Cannot set authenticated without valid token. Use constructor instead.");
    }
    super.setAuthenticated(isAuthenticated);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof JwtAuthenticationToken that) {
      return Objects.equals(this.token, that.token);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.token);
  }
}
