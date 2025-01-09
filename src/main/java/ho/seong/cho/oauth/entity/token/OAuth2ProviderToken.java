package ho.seong.cho.oauth.entity.token;

import ho.seong.cho.oauth.entity.OAuth2ProviderType;
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
}
