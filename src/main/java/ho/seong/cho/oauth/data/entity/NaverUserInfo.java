package ho.seong.cho.oauth.data.entity;

import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;

/**
 * 네이버 사용자 정보 DTO
 *
 * @param resultcode API 요청 결과 코드
 * @param message API 요청 결과 메시지
 * @param response 사용자 정보
 */
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

  /**
   * 네이버 사용자 정보
   *
   * @param id 사용자 고유 ID
   * @param email 사용자 이메일
   * @param nickname 사용자 닉네임
   * @param name 사용자 이름
   */
  private record Response(String id, String email, String nickname, String name) {}
}
