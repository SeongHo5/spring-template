package ho.seong.cho.oauth.data.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;

/**
 * GitHub 사용자 정보 DTO
 *
 * @param id GitHub 사용자 고유 ID
 * @param userId GitHub 사용자 ID
 * @param email GitHub 사용자 이메일
 */
public record GitHubUserInfo(Integer id, @JsonProperty("login") String userId, String email)
    implements OAuth2UserInfo {
  @Override
  public String getId() {
    return this.id.toString();
  }

  @Override
  public String getEmail() {
    return this.email;
  }

  @Override
  public OAuth2ProviderType getProvider() {
    return OAuth2ProviderType.GITHUB;
  }
}
