package ho.seong.cho.oauth;

import static ho.seong.cho.jwt.impl.JwtProperties.BEARER_PREFIX;

import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.data.OAuth2Properties;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public abstract class AbstractOAuth2Template implements OAuth2Template {

  protected static final Logger log = LoggerFactory.getLogger(AbstractOAuth2Template.class);

  protected final OAuth2Properties oAuth2Properties;
  protected final OAuth2ProviderTokenRepository providerTokenRepository;

  @Override
  public Boolean isAuthenticated(final String oAuthId) {
    return this.providerTokenRepository.existsById(oAuthId);
  }

  /**
   * 사용자의 OAuth2 인증 토큰 정보를 조회한다.
   *
   * @param userOAuthId 조회에 사용할 사용자의 OAuth2 ID
   * @return 사용자의 OAuth2 인증 토큰 정보
   * @throws RuntimeException 사용자의 OAuth2 인증 토큰 정보가 존재하지 않을 경우
   */
  protected OAuth2ProviderToken findToken(final String userOAuthId) {
    return this.providerTokenRepository.findById(userOAuthId).orElseThrow(RuntimeException::new);
  }

  /**
   * OAuth2 인증 토큰에 {@code Bearer} 접두어를 추가한다.
   *
   * @param token 접두어를 추가할 토큰
   * @return {@code Bearer} 접두어가 추가된 토큰
   */
  protected static String prependBearer(final String token) {
    return BEARER_PREFIX.concat(token);
  }
}
