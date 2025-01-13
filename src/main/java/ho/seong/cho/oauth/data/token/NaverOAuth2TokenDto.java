package ho.seong.cho.oauth.data.token;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
