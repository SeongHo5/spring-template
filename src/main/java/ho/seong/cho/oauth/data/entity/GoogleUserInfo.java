package ho.seong.cho.oauth.data.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;

/**
 * Google 사용자 정보 DTO
 *
 * @param id Google 사용자 고유 ID
 * @param email Google 사용자 이메일
 */
public record GoogleUserInfo(@JsonProperty("sub") String id, String email)
    implements OAuth2UserInfo {
  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getEmail() {
    return this.email;
  }

  @Override
  public OAuth2ProviderType getProvider() {
    return OAuth2ProviderType.GOOGLE;
  }
}
