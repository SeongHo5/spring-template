package ho.seong.cho.security.userdetails;

import ho.seong.cho.users.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface MyUserDetails extends UserDetails {

  User getUser();

  Long getId();

  boolean isSuperAdmin();

  boolean isAdmin();
}
