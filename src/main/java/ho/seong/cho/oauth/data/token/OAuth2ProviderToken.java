package ho.seong.cho.oauth.data.token;

import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Builder
@RedisHash("OAUTH2_PROVIDER_TOKEN")
public class OAuth2ProviderToken {

  @Id private String id;

  private OAuth2ProviderType providerType;

  private String accessToken;

  private String refreshToken;

  private String idToken;

  private Integer accessTokenExpiresIn;

  private Integer refreshTokenExpiresIn;

  @TimeToLive private Long timeToLive;

  public static OAuth2ProviderToken from(
      OAuth2ProviderType providerType, String oAuthId, OAuth2ProviderTokenDto providerTokenDto) {
    return builder()
        .id(oAuthId)
        .providerType(providerType)
        .accessToken(providerTokenDto.getAccessToken())
        .refreshToken(providerTokenDto.getRefreshToken())
        .idToken(providerTokenDto.getIdToken())
        .accessTokenExpiresIn(providerTokenDto.getAccessTokenExpiresIn())
        .refreshTokenExpiresIn(providerTokenDto.getRefreshTokenExpiresIn())
        .timeToLive(providerTokenDto.getAccessTokenExpiresIn().longValue())
        .build();
  }
}
