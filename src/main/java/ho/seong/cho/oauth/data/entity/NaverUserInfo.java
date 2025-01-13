package ho.seong.cho.oauth.data.entity;

import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;

public record NaverUserInfo(String resultcode, String message, Response response)
    implements OAuth2UserInfo {

  @Override
  public String getId() {
    return this.response.id;
  }

  @Override
  public String getEmail() {
    return this.response.email;
  }

  @Override
  public OAuth2ProviderType getProvider() {
    return OAuth2ProviderType.NAVER;
  }

  public String id() {
    return this.response.id;
  }

  private record Response(String id, String email, String nickname, String name) {}
}
