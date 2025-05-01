package ho.seong.cho.oauth.impl;

import ho.seong.cho.infra.client.http.GoogleOAuth2Client;
import ho.seong.cho.infra.client.http.GoogleUserClient;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.AbstractOAuth2Template;
import ho.seong.cho.oauth.OAuth2Properties;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.token.OAuth2ProviderTokenDto;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2Template extends AbstractOAuth2Template {

  private final GoogleOAuth2Client googleOAuth2Client;
  private final GoogleUserClient googleUserClient;

  public GoogleOAuth2Template(
      OAuth2Properties oAuth2Properties,
      OAuth2ProviderTokenRepository providerTokenRepository,
      GoogleOAuth2Client googleOAuth2Client,
      GoogleUserClient googleUserClient) {
    super(oAuth2Properties, providerTokenRepository);
    this.googleOAuth2Client = googleOAuth2Client;
    this.googleUserClient = googleUserClient;
  }

  @Override
  public OAuth2ProviderType getProviderType() {
    return OAuth2ProviderType.GOOGLE;
  }

  @Override
  protected OAuth2ProviderTokenDto exchangeCodeForToken(String code) {
    final var googleProperties = this.oAuth2Properties.google();
    return this.googleOAuth2Client.issueToken(
        OAuth2Properties.Google.GRANT_TYPE,
        googleProperties.clientId(),
        googleProperties.clientSecret(),
        googleProperties.redirectUri(),
        code);
  }

  @Override
  protected OAuth2UserInfo getUserInfoById(String oAuthId) {
    return this.getUserInfoByToken(super.loadAccessToken(oAuthId).getAccessToken());
  }

  @Override
  protected OAuth2UserInfo getUserInfoByToken(String token) {
    return this.googleUserClient.getUserInfo(withBearerPrefix(token));
  }

  @Override
  public void disconnect(final String oAuthId) {
    final var accessToken = super.loadAccessToken(oAuthId).getAccessToken();
    this.googleOAuth2Client.revokeToken(accessToken);
    this.providerTokenRepository.deleteById(oAuthId);
  }
}
