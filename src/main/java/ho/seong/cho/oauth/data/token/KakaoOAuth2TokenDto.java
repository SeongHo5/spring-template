package ho.seong.cho.oauth.data.token;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.annotation.Nullable;

/**
 * Kakao OAuth2 Response
 *
 * @param tokenType 토큰 타입, {@code Bearer}로 고정
 * @param accessToken 접근 토큰
 * @param idToken ID 토큰
 * @param expiresIn 접근 토큰 만료 시간(초)
 * @param refreshToken 리프레시 토큰
 * @param refreshTokenExpiresIn 리프레시 토큰 만료 시간(초)
 * @param scope 인증된 사용자의 조회 권한 범위
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoOAuth2TokenDto(
    String tokenType,
    String accessToken,
    @Nullable String idToken,
    Integer expiresIn,
    String refreshToken,
    Integer refreshTokenExpiresIn,
    @Nullable String scope)
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
    return this.idToken;
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
