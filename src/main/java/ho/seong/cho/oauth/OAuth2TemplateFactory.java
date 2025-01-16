package ho.seong.cho.oauth;

import ho.seong.cho.exception.custom.AuthenticationException;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import jakarta.validation.constraints.NotNull;

public interface OAuth2TemplateFactory {

  /**
   * {@link OAuth2ProviderType}에 대응하는 {@link OAuth2Template}을 반환한다.
   *
   * @param providerType 가져올 OAuth2 제공자 유형
   * @return 선택된 {@link OAuth2Template}
   */
  OAuth2Template getByProviderType(OAuth2ProviderType providerType);

  /**
   * 사용자의 OAuth2 인증 정보로 적절한 {@link OAuth2Template}을 찾아 반환한다.
   *
   * @param oAuthId 대상 사용자의 OAuth2 ID (never {@code null})
   * @return 선택된 {@link OAuth2Template}
   * @throws AuthenticationException OAuth2 인증 정보가 없거나 유효하지 않은 경우
   */
  OAuth2Template getByOAuthId(@NotNull String oAuthId);
}
