package ho.seong.cho.security.userdetails.impl;

import ho.seong.cho.security.userdetails.MyUserDetailsService;
import ho.seong.cho.users.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsServiceImpl implements MyUserDetailsService {

  private final UserService userService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return this.userService.findByEmail(username).map(MyUserDetailsImpl::from).orElseThrow();
  }

  @Override
  public Authentication createAuthentication(UserDetails userDetails) {
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }

  @Override
  public Authentication createAuthentication(Claims claims) {
    final String username = claims.getSubject();
    UserDetails userDetails = this.loadUserByUsername(username);
    return this.createAuthentication(userDetails);
  }

  @Override
  public String extractAuthority(Authentication authentication) {
    return authentication.getAuthorities().iterator().next().getAuthority();
  }
}
