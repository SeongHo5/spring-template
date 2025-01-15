package ho.seong.cho.oauth;

import ho.seong.cho.exception.custom.NoSuchEntityException;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import jakarta.validation.constraints.NotNull;

public interface OAuth2Template {

  /**
   * 인가 코드로 토큰을 발급한다.
   *
   * @param code 인가 코드
   * @return 발급된 {@link OAuth2ProviderToken}
   */
  OAuth2ProviderToken issueToken(@NotNull final String code);

  /**
   * 사용자가 인증되었는지 확인한다.
   *
   * @param oAuthId 확인할 사용자의 OAuth ID
   * @return 사용자가 인증되었는지 여부
   * @apiNote Cache에 저장된 유효한 OAuth 제공자 토큰이 있는지 여부로 확인한다.
   */
  boolean isAuthenticated(@NotNull final String oAuthId);

  /**
   * 사용자의 OAuth ID으로 사용자 정보를 가져온다.
   *
   * @param oAuthId 사용자의 OAuth ID
   * @return 사용자 정보
   * @apiNote Cache에 저장된 토큰을 사용하여 사용자 정보를 가져온다.
   * @throws NoSuchEntityException OAuth ID에 해당하는 토큰이 없을 경우
   */
  OAuth2UserInfo getUserInfo(@NotNull final String oAuthId);

  /**
   * 회원 탈퇴(OAuth2 연결 끊기, 토큰 삭제) 작업을 수행한다.
   *
   * @param oAuthId 탈퇴 처리할 사용자의 OAuth ID
   */
  void withdrawal(@NotNull final String oAuthId);
}
