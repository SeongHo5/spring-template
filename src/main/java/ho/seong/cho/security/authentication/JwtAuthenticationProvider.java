package ho.seong.cho.security.authentication;

import ho.seong.cho.jwt.JwtProvider;
import ho.seong.cho.users.UserService;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

  private final UserService userService;
  private final JwtProvider jwtProvider;

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthenticationToken.class.isAssignableFrom(authentication);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    return Optional.of(authentication)
        .map(JwtAuthenticationToken.class::cast)
        .map(JwtAuthenticationToken::getToken)
        .flatMap(this.jwtProvider::parse)
        .map(Claims::getSubject)
        .flatMap(this.userService::findByEmail)
        .map(JwtAuthenticationToken::from)
        .orElseThrow(() -> new BadCredentialsException("Invalid token"));
  }
}
