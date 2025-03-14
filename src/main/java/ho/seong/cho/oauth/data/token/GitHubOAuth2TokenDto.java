package ho.seong.cho.oauth.data.token;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.annotation.Nullable;

/**
 * GitHub OAuth2 Response
 *
 * @param tokenType 토큰 타입, {@code Bearer}로 고정
 * @param accessToken 접근 토큰
 * @param scope 접근 토큰의 범위
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitHubOAuth2TokenDto(
    String tokenType,
    String accessToken,
    Integer expiresIn,
    String refreshToken,
    Integer refreshTokenExpiresIn,
    String scope)
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
  @Nullable public String getIdToken() {
    return null;
  }

  @Override
  public Integer getAccessTokenExpiresIn() {
    return this.expiresIn;
  }

  @Override
  public Integer getRefreshTokenExpiresIn() {
    return this.refreshTokenExpiresIn;
  }
}
