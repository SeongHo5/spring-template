package ho.seong.cho.jwt.impl;

import static ho.seong.cho.jwt.impl.JwtProperties.*;

import ho.seong.cho.jwt.JsonWebToken;
import ho.seong.cho.jwt.JwtProvider;
import ho.seong.cho.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProviderImpl implements JwtProvider {

  private final Clock clock;
  private final JwtProperties jwtProperties;
  private final UserDetailsService userDetailsService;

  @Override
  public JsonWebToken create(User user) {
    UserDetails userDetails = this.userDetailsService.loadUserByUsername(user.getName());

    final var now = this.getCurrentDate();
    final var accessToken = this.createAccessToken(userDetails, now);
    final var refreshToken = this.createRefreshToken(user.getName(), now);

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

    return JsonWebToken.builder()
        .accessToken(this.createAccessToken(claims, this.getCurrentDate()))
        .accessTokenExpiresIn(ACCESS_TOKEN_EXPIRATION.toSeconds())
        .build();
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

  private String createAccessToken(UserDetails userDetails, Date now) {
    return this.createAccessToken(
        userDetails.getUsername(),
        userDetails.getAuthorities().iterator().next().getAuthority(),
        now);
  }

  private String createAccessToken(Claims claims, Date now) {
    return this.createAccessToken(
        claims.getSubject(), claims.get(AUTHENTICATION_KEY, String.class), now);
  }

  private String createAccessToken(String username, String claim, Date now) {
    return Jwts.builder()
        .issuer(this.jwtProperties.issuer())
        .subject(username)
        .issuedAt(now)
        .notBefore(now)
        .claim(AUTHENTICATION_KEY, claim)
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

  private Date getCurrentDate() {
    return Date.from(Instant.now(this.clock));
  }
}
