package ho.seong.cho.security;

import ho.seong.cho.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface MyUserDetails extends UserDetails {

  User getUser();

  Long getId();

  boolean isAdmin();
}
