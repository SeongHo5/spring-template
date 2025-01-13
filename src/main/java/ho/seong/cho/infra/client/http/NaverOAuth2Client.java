package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.token.NaverOAuth2TokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "naverOAuth2Client", url = "https://nid.naver.com")
public interface NaverOAuth2Client {

  @PostMapping("/oauth2.0/token")
  NaverOAuth2TokenDto issueToken(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code,
      @RequestParam("state") String state);

  @PostMapping("/oauth2.0/token")
  NaverOAuth2TokenDto renewToken(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("refresh_token") String refreshToken);

  @PostMapping("/oauth2.0/token")
  void withdrawal(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("access_token") String accessToken,
      @RequestParam("service_provider") String serviceProvider);
}
