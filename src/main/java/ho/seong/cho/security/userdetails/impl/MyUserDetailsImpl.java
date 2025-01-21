package ho.seong.cho.security.userdetails.impl;

import ho.seong.cho.security.userdetails.MyUserDetails;
import ho.seong.cho.users.User;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;

public record MyUserDetailsImpl(User user) implements MyUserDetails, Serializable {

  @Serial private static final long serialVersionUID = 2025010101L;

  public static MyUserDetailsImpl from(User user) {
    return new MyUserDetailsImpl(user);
  }

  @Override
  public User getUser() {
    return this.user;
  }

  @Override
  public String getUsername() {
    return this.user.getName();
  }

  @Override
  public Long getId() {
    return this.user.getId();
  }

  @Override
  public boolean isSuperAdmin() {
    return this.user.isSuperAdmin();
  }

  @Override
  public boolean isAdmin() {
    return this.user.isAdmin();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton((GrantedAuthority) () -> this.user.getRoleType().name());
  }

  @Override
  public String getPassword() {
    throw new UnsupportedOperationException();
  }
}
