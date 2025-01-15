package ho.seong.cho.oauth.data.token;

import jakarta.annotation.Nullable;

/** OAuth2 Provider별 응답을 담는 인터페이스 */
public sealed interface OAuth2ProviderTokenDto
    permits AppleOAuth2TokenDto,
        GoogleOAuth2TokenDto,
        KakaoOAuth2TokenDto,
        NaverOAuth2TokenDto,
        GitHubOAuth2TokenDto {

  /**
   * OAuth2 제공자가 발급한 접근 토큰(access token)을 반환한다.
   *
   * @return 접근 토큰
   */
  String getAccessToken();

  /**
   * OAuth2 제공자가 발급한 갱신 토큰(refresh token)을 반환한다.
   *
   * @return 갱신 토큰
   */
  String getRefreshToken();

  /**
   * OAuth2 제공자가 발급한 ID 토큰(ID token)을 반환한다.
   *
   * @return ID 토큰 / 제공자가 OIDC를 지원하지 않는 경우 null
   */
  @Nullable String getIdToken();

  /**
   * OAuth2 제공자가 발급한 접근 토큰의 만료 시간을 반환한다.
   *
   * @return 접근 토큰 만료 시간(초)
   */
  Integer getAccessTokenExpiresIn();

  /**
   * OAuth2 제공자가 발급한 갱신 토큰의 만료 시간을 반환한다.
   *
   * @return 갱신 토큰 만료 시간(초) / 제공자가 명시적으로 만료 시간을 지정하지 않는 경우 {@link Integer#MAX_VALUE}
   */
  Integer getRefreshTokenExpiresIn();
}
