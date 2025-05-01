package ho.seong.cho.oauth;

import static ho.seong.cho.jwt.impl.JwtProperties.BEARER_PREFIX;

import ho.seong.cho.exception.custom.NoSuchEntityException;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import ho.seong.cho.oauth.data.token.OAuth2ProviderTokenDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public abstract class AbstractOAuth2Template implements OAuth2Template {

  protected static final Logger log = LoggerFactory.getLogger(AbstractOAuth2Template.class);

  protected final OAuth2Properties oAuth2Properties;
  protected final OAuth2ProviderTokenRepository providerTokenRepository;

  /**
   * {@inheritDoc}
   *
   * @param oAuthId 확인할 사용자의 OAuth ID
   * @return 사용자가 인증되었는지 여부
   * @apiNote Cache에 저장된 유효한 OAuth 제공자 토큰이 있는지 여부로 확인한다.
   */
  @Override
  public boolean isAuthorized(final String oAuthId) {
    return this.providerTokenRepository.existsById(oAuthId);
  }

  /**
   * {@inheritDoc}
   *
   * @param code 인가 코드
   * @return 발급된 {@link OAuth2ProviderToken}
   */
  @Override
  public OAuth2ProviderToken authorize(final String code) {
    final var providerTokenDto = this.exchangeCodeForToken(code);
    final var oAuthId = this.getUserInfoByToken(providerTokenDto.getAccessToken()).getId();
    return this.providerTokenRepository.save(
        OAuth2ProviderToken.from(this.getProviderType(), oAuthId, providerTokenDto));
  }

  /**
   * {@inheritDoc}
   *
   * @param oAuthId 사용자의 OAuth ID
   * @return 사용자 정보
   * @apiNote Cache에 저장된 토큰을 사용하여 사용자 정보를 가져온다.
   * @throws NoSuchEntityException OAuth ID에 해당하는 토큰이 없을 경우
   */
  @Override
  public OAuth2UserInfo fetchUserInfo(final String oAuthId) {
    return this.getUserInfoById(oAuthId);
  }

  /**
   * 사용자의 OAuth2 인증 토큰 정보를 조회한다.
   *
   * @param oAuthId 조회에 사용할 사용자의 OAuth2 ID
   * @return 사용자의 OAuth2 인증 토큰 정보
   * @throws RuntimeException 사용자의 OAuth2 인증 토큰 정보가 존재하지 않을 경우
   */
  protected OAuth2ProviderToken loadAccessToken(final String oAuthId) {
    return this.providerTokenRepository.findById(oAuthId).orElseThrow(RuntimeException::new);
  }

  /**
   * OAuth2 인증 토큰에 {@code Bearer} 접두어를 추가한다.
   *
   * @param token 접두어를 추가할 토큰
   * @return {@code Bearer} 접두어가 추가된 토큰
   */
  protected static String withBearerPrefix(final String token) {
    return BEARER_PREFIX.concat(token);
  }

  /**
   * 인가 코드를 OAuth2 제공자에 전달하여 액세스 토큰(ID 토큰 포함)을 교환한다.
   *
   * @param code 클라이언트가 받은 인가 코드
   * @return 제공자에서 응답받은 액세스 토큰 및 관련 정보 DTO
   */
  protected abstract OAuth2ProviderTokenDto exchangeCodeForToken(final String code);

  /**
   * 사용자 OAuth ID를 기반으로 사용자 정보를 조회한다.
   *
   * @param oAuthId 사용자 식별자 (OAuth2 ID)
   * @return 외부 OAuth2 제공자에서 조회된 사용자 정보
   * @throws RuntimeException 저장된 토큰이 존재하지 않거나 조회에 실패한 경우
   */
  protected abstract OAuth2UserInfo getUserInfoById(String oAuthId);

  /**
   * 주어진 액세스 토큰 또는 ID 토큰을 사용하여 OAuth2 제공자로부터 사용자 정보를 조회한다.
   *
   * @param token 액세스 토큰 또는 ID 토큰
   * @return 외부 OAuth2 제공자에서 조회된 사용자 정보
   */
  protected abstract OAuth2UserInfo getUserInfoByToken(String token);
}
