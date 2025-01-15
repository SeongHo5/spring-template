package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.token.GoogleOAuth2TokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "googleOAuth2Client", url = "https://accounts.google.com/o/oauth2")
public interface GoogleOAuth2Client {

  @PostMapping("/token")
  GoogleOAuth2TokenDto issueToken(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code);

  @PostMapping("/revoke")
  void revokeToken(@RequestParam("token") String token);
}
