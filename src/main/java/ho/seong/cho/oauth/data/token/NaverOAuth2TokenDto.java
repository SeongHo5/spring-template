package ho.seong.cho.oauth.data.token;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Naver OAuth2 Response
 *
 * @param tokenType 토큰 타입, {@code Bearer}로 고정
 * @param accessToken 접근 토큰
 * @param expiresIn 접근 토큰 만료 시간(초)
 * @param refreshToken 갱신 토큰
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NaverOAuth2TokenDto(
    String tokenType, String accessToken, Integer expiresIn, String refreshToken)
    implements OAuth2ProviderTokenDto {

  @Override
  public String getAccessToken() {
    return this.accessToken;
  }

  @Override
  public String getRefreshToken() {
    return this.refreshToken;
  }

  @Override
  public String getIdToken() {
    return null;
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
