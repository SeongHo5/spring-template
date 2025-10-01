package ho.seong.cho.oauth.support;

import ho.seong.cho.exception.custom.InternalProcessingException;
import ho.seong.cho.oauth.OAuth2Properties;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public final class AppleClientSecretProvider {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private AppleClientSecretProvider() {}

  /**
   * Apple OAuth2 인증 요청에 사용할 Client Secret을 생성한다.
   *
   * @param keyId Apple Key ID
   * @param teamId Apple Team ID
   * @param clientId Client ID(App ID 또는 Services ID)
   * @param keyPath Auth Key 파일 경로
   * @return 생성된 Client Secret
   *     <ul>
   *       <li>JWS 헤더를 설정한다. (알고리즘과 키 ID 포함)
   *       <li>JWT의 클레임을 설정한다. (Issuer, Subject, Audience 등)
   *       <li>Apple 개인 키를 이용하여 JWT에 서명하고, 문자열 형태의 토큰을 생성한다.
   *       <li>getAuthKey 메서드를 통해 Apple의 개인 키를 파일에서 읽어옵니다.
   *       <li>읽어온 키를 PrivateKey 객체로 변환한다.
   *     </ul>
   */
  public static String create(
      final String keyId, final String teamId, final String clientId, final String keyPath) {
    return Jwts.builder()
        .header()
        .keyId(keyId)
        .and()
        .issuer(teamId)
        .issuedAt(new Date())
        .subject(clientId)
        .audience()
        .add(OAuth2Properties.Apple.AUDIENCE)
        .and()
        .expiration(createExpirationDate())
        .signWith(getAuthKey(keyPath), Jwts.SIG.ES256)
        .compact();
  }

  /**
   * Apple OAuth2 인증 요청에 사용할 Client Secret을 생성한다.
   *
   * @param appleProperties OAuth2 설정 정보
   * @return 생성된 Client Secret
   * @apiNote 내부적으로 {@link #create} 에게 위임한다.
   */
  public static String create(OAuth2Properties.Apple appleProperties) {
    return create(
        appleProperties.keyId(),
        appleProperties.teamId(),
        appleProperties.clientId(),
        appleProperties.keyPath());
  }

  private static PrivateKey getAuthKey(final String appleKeyPath) {
    var resource = new ClassPathResource(appleKeyPath);

    try (var pemReader = new PemReader(new InputStreamReader(resource.getInputStream()))) {
      var pemObject = pemReader.readPemObject();
      byte[] keyBytes = pemObject.getContent();

      var spec = new PKCS8EncodedKeySpec(keyBytes);
      return KeyFactory.getInstance("EC").generatePrivate(spec);
    } catch (IOException | GeneralSecurityException ex) {
      log.error("Failed to read Apple private key / Reason: {}", ex.getMessage());
      throw new InternalProcessingException(ex);
    }
  }

  private static Date createExpirationDate() {
    return Date.from(LocalDateTime.now().plusMonths(3).atZone(ZoneId.systemDefault()).toInstant());
  }
}
