package ho.seong.cho.oauth.data.token;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Apple OAuth2 Response
 *
 * @param tokenType 토큰 타입, {@code Bearer}로 고정
 * @param accessToken 접근 토큰 (<i>미사용</i>)
 * @param idToken ID 토큰
 * @param refreshToken 리프레시 토큰
 * @param expiresIn 접근 토큰 만료 시간(초)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AppleOAuth2TokenDto(
    String tokenType, String accessToken, String idToken, String refreshToken, Integer expiresIn)
    implements OAuth2ProviderTokenDto {

  /**
   * Apple OAuth2에서는 접근 토큰 대신 ID 토큰을 사용하므로, 이 메서드는 ID 토큰을 반환한다.
   *
   * @return ID Token (<strong>NOT</strong> Acess Token!)
   */
  @Override
  public String getAccessToken() {
    return this.idToken;
  }

  @Override
  public String getRefreshToken() {
    return this.refreshToken;
  }

  @Override
  public String getIdToken() {
    return this.idToken;
  }

  @Override
  public Integer getAccessTokenExpiresIn() {
    return this.expiresIn;
  }

  @Override
  public Integer getRefreshTokenExpiresIn() {
    return Integer.MAX_VALUE;
  }
}
