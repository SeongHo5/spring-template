package ho.seong.cho.security.userdetails.impl;

import ho.seong.cho.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserService userService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return this.userService
        .findByEmail(username)
        .map(MyUserDetailsImpl::from)
        .orElseThrow(() -> new UsernameNotFoundException("No user found with name: " + username));
  }
}
