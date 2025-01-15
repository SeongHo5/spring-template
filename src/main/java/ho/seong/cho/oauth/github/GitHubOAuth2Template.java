package ho.seong.cho.oauth.github;

import ho.seong.cho.infra.client.http.GitHubOAuth2Client;
import ho.seong.cho.infra.client.http.GitHubUserClient;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.AbstractOAuth2Template;
import ho.seong.cho.oauth.data.OAuth2Properties;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.internal.OAuth2WithdrawalRequest;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
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
  public OAuth2ProviderToken issueToken(String code) {
    OAuth2Properties.GitHub gitHubProperties = this.oAuth2Properties.github();
    OAuth2ProviderTokenDto tokenDto =
        this.gitHubOAuth2Client.issueToken(
            gitHubProperties.clientId(),
            gitHubProperties.clientSecret(),
            gitHubProperties.redirectUri(),
            code);
    final String oAuthId = this.gitHubUserClient.getUserInfo(tokenDto.getAccessToken()).getId();
    return super.providerTokenRepository.save(
        OAuth2ProviderToken.from(OAuth2ProviderType.GITHUB, oAuthId, tokenDto));
  }

  @Override
  public OAuth2UserInfo getUserInfo(String oAuthId) {
    final String accessToken = super.findToken(oAuthId).getAccessToken();
    return this.gitHubUserClient.getUserInfo(accessToken);
  }

  @Override
  public void withdrawal(String oAuthId) {
    final String accessToken = super.findToken(oAuthId).getAccessToken();
    this.gitHubUserClient.withdrawal(
        this.oAuth2Properties.github().clientId(), OAuth2WithdrawalRequest.from(accessToken));
    this.providerTokenRepository.deleteById(oAuthId);
  }
}
