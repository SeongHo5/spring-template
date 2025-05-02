package ho.seong.cho.oauth.impl;

import ho.seong.cho.infra.client.http.GitHubOAuth2Client;
import ho.seong.cho.infra.client.http.GitHubUserClient;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.AbstractOAuth2Template;
import ho.seong.cho.oauth.OAuth2Properties;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.internal.OAuth2WithdrawalRequest;
import ho.seong.cho.oauth.data.token.OAuth2ProviderTokenDto;
import org.springframework.stereotype.Component;

@Component
public class GitHubOAuth2Template extends AbstractOAuth2Template {

  private final GitHubOAuth2Client gitHubOAuth2Client;
  private final GitHubUserClient gitHubUserClient;

  public GitHubOAuth2Template(
      OAuth2Properties oAuth2Properties,
      OAuth2ProviderTokenRepository providerTokenRepository,
      GitHubOAuth2Client gitHubOAuth2Client,
      GitHubUserClient gitHubUserClient) {
    super(oAuth2Properties, providerTokenRepository);
    this.gitHubOAuth2Client = gitHubOAuth2Client;
    this.gitHubUserClient = gitHubUserClient;
  }

  @Override
  public OAuth2ProviderType getProviderType() {
    return OAuth2ProviderType.GITHUB;
  }

  @Override
  protected OAuth2ProviderTokenDto exchangeCodeForToken(String code) {
    final var gitHubProperties = this.oAuth2Properties.github();
    return this.gitHubOAuth2Client.issueToken(
        gitHubProperties.clientId(),
        gitHubProperties.clientSecret(),
        gitHubProperties.redirectUri(),
        code);
  }

  @Override
  protected OAuth2UserInfo getUserInfoById(String oAuthId) {
    return this.getUserInfoByToken(super.loadAccessToken(oAuthId).getAccessToken());
  }

  @Override
  protected OAuth2UserInfo getUserInfoByToken(String token) {
    return this.gitHubUserClient.getUserInfo(withBearerPrefix(token));
  }

  @Override
  public void disconnect(final String oAuthId) {
    final var accessToken = super.loadAccessToken(oAuthId).getAccessToken();
    this.gitHubUserClient.withdrawal(
        this.oAuth2Properties.github().clientId(), OAuth2WithdrawalRequest.from(accessToken));
    this.providerTokenRepository.deleteById(oAuthId);
  }
}
