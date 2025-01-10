package ho.seong.cho.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * JWT 관련 설정 정보 Properties
 *
 * @param issuer 발급자(issuer) 정보
 * @param secret 서명에 사용할 비밀키
 * @param key 서명에 사용할 {@link Key}
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String issuer, String secret, SecretKey key) {

  /** JWT에서 사용자 권한 정보를 저장하는 클레임 키 */
  static final String AUTHENTICATION_KEY = "auth";

  /** JWT에서 사용자 이름을 저장하는 클레임 키 */
  static final String USER_NAME_KEY = "name";

  /** 액세스 토큰의 기본 만료 시간 */
  static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(2);

  /** 리프레시 토큰의 기본 만료 시간 */
  static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

  /** JWT 서명에 사용할 알고리즘 */
  static final MacAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.HS512;

  @ConstructorBinding
  public JwtProperties(String issuer, String secret) {
    this(issuer, secret, Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)));
  }
}
