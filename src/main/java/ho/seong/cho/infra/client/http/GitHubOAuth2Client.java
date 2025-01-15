package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.token.GitHubOAuth2TokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "gitHubOAuth2Client", url = "https://github.com")
public interface GitHubOAuth2Client {

  @PostMapping("/login/oauth/acces_token")
  GitHubOAuth2TokenDto issueToken(
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code);
}
