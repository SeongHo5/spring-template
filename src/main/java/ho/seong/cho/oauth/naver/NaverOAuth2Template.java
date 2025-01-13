package ho.seong.cho.oauth.naver;

import ho.seong.cho.infra.client.http.NaverOAuth2Client;
import ho.seong.cho.infra.client.http.NaverUserClient;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.AbstractOAuth2Template;
import ho.seong.cho.oauth.data.OAuth2Properties;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import ho.seong.cho.oauth.data.token.OAuth2ProviderTokenDto;
import org.springframework.stereotype.Component;

@Component
public class NaverOAuth2Template extends AbstractOAuth2Template {

  private final NaverOAuth2Client naverOAuth2Client;
  private final NaverUserClient naverUserClient;

  public NaverOAuth2Template(
      OAuth2Properties oAuth2Properties,
      OAuth2ProviderTokenRepository providerTokenRepository,
      NaverOAuth2Client naverOAuth2Client,
      NaverUserClient naverUserClient) {
    super(oAuth2Properties, providerTokenRepository);
    this.naverOAuth2Client = naverOAuth2Client;
    this.naverUserClient = naverUserClient;
  }

  @Override
  public OAuth2ProviderToken issueToken(String code) {
    OAuth2Properties.Naver naverProperties = this.oAuth2Properties.naver();
    OAuth2ProviderTokenDto tokenDto =
        this.naverOAuth2Client.issueToken(
            OAuth2Properties.Naver.GRANT_TYPE_ISSUE,
            naverProperties.clientId(),
            naverProperties.clientSecret(),
            naverProperties.redirectUri(),
            code,
            null);
    final String oAuthId =
        this.naverUserClient.getUserInfo(prependBearer(tokenDto.getAccessToken())).id();
    return super.providerTokenRepository.save(
        OAuth2ProviderToken.from(OAuth2ProviderType.NAVER, oAuthId, tokenDto));
  }

  @Override
  public OAuth2UserInfo fetchUserInfo(String oAuthId) {
    final String accessToken = super.findToken(oAuthId).getAccessToken();
    return this.naverUserClient.getUserInfo(prependBearer(accessToken));
  }

  @Override
  public void withdrawal(String oAuthId) {
    final String accessToken = super.findToken(oAuthId).getAccessToken();
    this.naverOAuth2Client.withdrawal(
        OAuth2Properties.Naver.GRANT_TYPE_WITHDRAWAL,
        this.oAuth2Properties.naver().clientId(),
        this.oAuth2Properties.naver().clientSecret(),
        accessToken,
        OAuth2ProviderType.NAVER.name());
    this.providerTokenRepository.deleteById(oAuthId);
  }
}
