package ho.seong.cho.oauth.impl;

import ho.seong.cho.infra.client.http.AppleOAuth2Client;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.AbstractOAuth2Template;
import ho.seong.cho.oauth.OAuth2Properties;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.internal.OAuth2ProviderJsonWebKeys;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import ho.seong.cho.oauth.data.token.OAuth2ProviderTokenDto;
import ho.seong.cho.oauth.support.AppleClientSecretProvider;
import ho.seong.cho.oauth.support.OpenIdConnectTokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppleOAuth2Template extends AbstractOAuth2Template {

  private final AppleOAuth2Client appleOAuth2Client;

  public AppleOAuth2Template(
      OAuth2Properties oAuth2Properties,
      AppleOAuth2Client appleOAuth2Client,
      OAuth2ProviderTokenRepository providerTokenRepository) {
    super(oAuth2Properties, providerTokenRepository);
    this.appleOAuth2Client = appleOAuth2Client;
  }

  @Override
  public OAuth2ProviderType getProviderType() {
    return OAuth2ProviderType.APPLE;
  }

  @Override
  protected OAuth2ProviderTokenDto exchangeCodeForToken(String code) {
    final var appleProperties = this.oAuth2Properties.apple();
    final var clientSecret = AppleClientSecretProvider.create(appleProperties);
    return this.appleOAuth2Client.issueOrRenewToken(
        appleProperties.clientId(),
        clientSecret,
        code,
        OAuth2Properties.Apple.GRANT_TYPE,
        appleProperties.redirectUri(),
        null);
  }

  @Override
  protected OAuth2UserInfo getUserInfoById(String oAuthId) {
    return this.getUserInfoByToken(super.loadAccessToken(oAuthId).getIdToken());
  }

  @Override
  protected OAuth2UserInfo getUserInfoByToken(String idToken) {
    final var jsonWebKey = this.getMatchingJsonWebKey(idToken);
    return OpenIdConnectTokenUtils.parse(idToken, jsonWebKey.n(), jsonWebKey.e());
  }

  @Override
  @Transactional
  public void disconnect(final String oAuthId) {
    final String clientSecret = AppleClientSecretProvider.create(super.oAuth2Properties.apple());
    OAuth2ProviderToken providerToken = super.loadAccessToken(oAuthId);
    this.appleOAuth2Client.revokeToken(
        super.oAuth2Properties.apple().clientId(),
        clientSecret,
        providerToken.getAccessToken(),
        OAuth2Properties.Apple.ACCESS_TOKEN_HINT);
    this.appleOAuth2Client.revokeToken(
        super.oAuth2Properties.apple().clientId(),
        clientSecret,
        providerToken.getRefreshToken(),
        OAuth2Properties.Apple.REFRESH_TOKEN_HINT);
    this.providerTokenRepository.delete(providerToken);
  }

  /** Apple의 공개키 중, idToken 암호화 방식과 일치하는 공개키를 찾아 반환한다. */
  private OAuth2ProviderJsonWebKeys.JsonWebKey getMatchingJsonWebKey(final String idToken) {
    final var keyId =
        OpenIdConnectTokenUtils.parseKeyIdHeader(
            idToken, OAuth2Properties.Apple.AUDIENCE, this.oAuth2Properties.apple().clientId());
    return this.appleOAuth2Client.getPublicKeys().keys().stream()
        .filter(jwk -> jwk.kid().equals(keyId))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Cannot Process JWT: No matching algorithm"));
  }
}
