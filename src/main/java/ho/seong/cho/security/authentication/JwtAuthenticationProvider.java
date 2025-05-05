package ho.seong.cho.security.authentication;

import ho.seong.cho.jwt.JwtProvider;
import ho.seong.cho.security.userdetails.MyUserDetails;
import ho.seong.cho.users.UserService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    if (!(authentication instanceof JwtAuthenticationToken)) {
      throw new AuthenticationCredentialsNotFoundException("JwtAuthenticationToken is required");
    }
    String token = authentication.getCredentials().toString();
    long userId = Long.parseLong(this.validateAndGetClaims(token).getSubject());

    return this.userService
        .findById(userId)
        // .filter(User::isEnabled)
        .map(user -> JwtAuthenticationToken.authenticated(MyUserDetails.from(user)))
        .orElseThrow(() -> new UsernameNotFoundException("User not found or disabled"));
  }

  private Claims validateAndGetClaims(String token) {
    return this.jwtProvider
        .parse(token)
        .filter(this::isNotExpired)
        .orElseThrow(() -> new BadCredentialsException("Token is expired or invalid"));
  }

  private boolean isNotExpired(Claims claims) {
    var expirationTime = claims.getExpiration().toInstant();
    return Instant.now().isBefore(expirationTime);
  }
}
