package ho.seong.cho.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 Client Properties 관리 클래스
 *
 * @param kakao Kakao OAuth2 관련 설정
 * @param naver Naver OAuth2 관련 설정
 * @param apple Apple OAuth2 관련 설정
 * @param google Google OAuth2 관련 설정
 * @param github GitHub OAuth2 관련 설정
 */
@ConfigurationProperties(prefix = "security.oauth2.client")
public record OAuth2Properties(
    Kakao kakao, Naver naver, Apple apple, Google google, GitHub github) {

  /**
   * Kakao OAuth2 Properties
   *
   * @param adminKey Kakao Admin Key
   * @param clientId Kakao Client ID
   * @param clientSecret Kakao Client Secret
   * @param redirectUri Kakao Redirect URI
   */
  public record Kakao(String adminKey, String clientId, String clientSecret, String redirectUri) {

    public static final String ADMIN_KEY_PREFIX = "KakaoAK ";

    public static final String GRANT_TYPE = "authorization_code";

    public static final String TARGET_ID_TYPE = "user_id";
  }

  /**
   * Naver OAuth2 Properties
   *
   * @param clientId Naver Client ID
   * @param clientSecret Naver Client Secret
   * @param redirectUri Naver Redirect URI
   */
  public record Naver(String clientId, String clientSecret, String redirectUri) {

    public static final String GRANT_TYPE_ISSUE = "authorization_code";

    public static final String GRANT_TYPE_RENEW = "refresh_token";

    public static final String GRANT_TYPE_WITHDRAWAL = "delete";
  }

  /**
   * Apple OAuth2 Properties
   *
   * @param teamId Apple Team ID
   * @param clientId Client ID(App ID 또는 Services ID)
   * @param redirectUri Apple Redirect URI
   * @param keyId Apple Key ID
   * @param keyPath Auth Key 파일 경로
   */
  public record Apple(
      String teamId, String clientId, String redirectUri, String keyId, String keyPath) {

    public static final String AUDIENCE = "https://appleid.apple.com";

    public static final String GRANT_TYPE = "authorization_code";

    public static final String ACCESS_TOKEN_HINT = "access_token";

    public static final String REFRESH_TOKEN_HINT = "refresh_token";
  }

  /**
   * Google OAuth2 Properties
   *
   * @param clientId Google Client ID
   * @param clientSecret Google Client Secret
   * @param redirectUri Google Redirect URI
   */
  public record Google(String clientId, String clientSecret, String redirectUri) {

    public static final String GRANT_TYPE = "authorization_code";
  }

  /**
   * GitHub OAuth2 Properties
   *
   * @param clientId GitHub Client ID
   * @param clientSecret GitHub Client Secret
   * @param redirectUri GitHub Redirect URI
   */
  public record GitHub(String clientId, String clientSecret, String redirectUri) {}
}
