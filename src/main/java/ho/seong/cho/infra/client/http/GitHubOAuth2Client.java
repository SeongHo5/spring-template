package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.token.GitHubOAuth2TokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "gitHubOAuth2Client", url = "https://github.com")
public interface GitHubOAuth2Client {

  /**
   * GitHub 접근 토큰 발급
   *
   * @param clientId 클라이언트 ID
   * @param clientSecret 클라이언트 시크릿
   * @param redirectUri 리다이렉트 URI
   * @param code 인가 코드
   * @return 발급된 접근 토큰
   */
  @PostMapping("/login/oauth/access_token")
  GitHubOAuth2TokenDto issueToken(
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code);
}
