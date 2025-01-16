package ho.seong.cho.oauth.impl;

import ho.seong.cho.infra.client.http.KakaoOAuth2Client;
import ho.seong.cho.infra.client.http.KakaoUserClient;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.AbstractOAuth2Template;
import ho.seong.cho.oauth.OAuth2Properties;
import ho.seong.cho.oauth.data.entity.OAuth2UserInfo;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import ho.seong.cho.oauth.data.token.OAuth2ProviderTokenDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KakaoOAuth2Template extends AbstractOAuth2Template {

  private final KakaoUserClient kakaoUserClient;
  private final KakaoOAuth2Client kakaoOAuth2Client;

  public KakaoOAuth2Template(
      OAuth2Properties oAuth2Properties,
      OAuth2ProviderTokenRepository providerTokenRepository,
      KakaoUserClient kakaoUserClient,
      KakaoOAuth2Client kakaoOAuth2Client) {
    super(oAuth2Properties, providerTokenRepository);
    this.kakaoUserClient = kakaoUserClient;
    this.kakaoOAuth2Client = kakaoOAuth2Client;
  }

  @Override
  public OAuth2ProviderType getProviderType() {
    return OAuth2ProviderType.KAKAO;
  }

  @Override
  public OAuth2ProviderToken issueToken(final String code) {
    OAuth2Properties.Kakao kakaoProperties = this.oAuth2Properties.kakao();
    OAuth2ProviderTokenDto tokenDto =
        this.kakaoOAuth2Client.issueOrRenewToken(
            OAuth2Properties.Kakao.GRANT_TYPE,
            kakaoProperties.clientId(),
            kakaoProperties.clientSecret(),
            kakaoProperties.redirectUri(),
            code);
    final String oAuthId =
        this.kakaoUserClient.getUserInfo(prependBearer(tokenDto.getAccessToken())).getId();
    return super.providerTokenRepository.save(
        OAuth2ProviderToken.from(OAuth2ProviderType.KAKAO, oAuthId, tokenDto));
  }

  @Override
  public OAuth2UserInfo getUserInfo(final String oAuthId) {
    final String accessToken = super.findToken(oAuthId).getAccessToken();
    return this.kakaoUserClient.getUserInfo(prependBearer(accessToken));
  }

  @Override
  @Transactional
  public void withdrawal(final String oAuthId) {
    final String accessToken = super.findToken(oAuthId).getAccessToken();
    this.kakaoUserClient.withdrawal(prependBearer(accessToken));
    this.providerTokenRepository.deleteById(oAuthId);
  }
}
