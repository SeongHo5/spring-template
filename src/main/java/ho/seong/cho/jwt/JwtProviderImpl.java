package ho.seong.cho.jwt;

import static ho.seong.cho.jwt.JwtProperties.*;

import ho.seong.cho.entity.User;
import ho.seong.cho.security.MyUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProviderImpl implements JwtProvider {

  private final Clock clock;
  private final JwtProperties jwtProperties;
  private final MyUserDetailsService userDetailsService;

  @Override
  public JsonWebToken create(User user) {
    UserDetails userDetails = this.userDetailsService.loadUserByUsername(user.getName());
    Authentication authentication = this.userDetailsService.createAuthentication(userDetails);

    final Date now = this.getCurrentTime();
    final String accessToken = this.createAccessToken(authentication, now);
    final String refreshToken = this.createRefreshToken(user.getName(), now);

    return JsonWebToken.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .accessTokenExpiresIn(ACCESS_TOKEN_EXPIRATION.toSeconds())
        .refreshTokenExpiresIn(REFRESH_TOKEN_EXPIRATION.toSeconds())
        .build();
  }

  @Override
  public JsonWebToken renew(String oldAccessToken, String refreshToken) {
    Claims claims =
        this.parse(refreshToken).orElseThrow(() -> new RuntimeException("Claims not found"));

    // Validations here...

    Authentication authentication = this.userDetailsService.createAuthentication(claims);
    return JsonWebToken.builder()
        .accessToken(this.createAccessToken(authentication, this.getCurrentTime()))
        .accessTokenExpiresIn(ACCESS_TOKEN_EXPIRATION.toSeconds())
        .build();
  }

  @Override
  @Nullable public String resolve(HttpServletRequest request) {
    final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }

    return authorization.substring(BEARER_PREFIX.length());
  }

  @Override
  public Optional<Claims> parse(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .requireIssuer(this.jwtProperties.issuer())
              .verifyWith(this.jwtProperties.key())
              .build()
              .parseSignedClaims(token)
              .getPayload();
      return Optional.of(claims);
    } catch (ExpiredJwtException ex) {
      return Optional.of(ex.getClaims());
    } catch (JwtException ex) {
      log.debug("Could not parse JWT Claims. / Reason: {}", ex.getMessage());
      return Optional.empty();
    }
  }

  private String createAccessToken(Authentication authentication, Date now) {
    return Jwts.builder()
        .issuer(this.jwtProperties.issuer())
        .subject(authentication.getName())
        .issuedAt(now)
        .notBefore(now)
        .claim(AUTHENTICATION_KEY, this.userDetailsService.extractAuthority(authentication))
        .expiration(this.toDate(ACCESS_TOKEN_EXPIRATION))
        .signWith(this.jwtProperties.key())
        .compact();
  }

  private String createRefreshToken(String username, Date now) {
    return Jwts.builder()
        .issuer(this.jwtProperties.issuer())
        .subject(username)
        .issuedAt(now)
        .notBefore(now)
        .expiration(this.toDate(REFRESH_TOKEN_EXPIRATION))
        .signWith(this.jwtProperties.key())
        .compact();
  }

  private Date toDate(Duration duration) {
    return Date.from(Instant.now(this.clock).plus(duration));
  }

  private Date getCurrentTime() {
    return Date.from(Instant.now(this.clock));
  }
}
