package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.entity.GitHubUserInfo;
import ho.seong.cho.oauth.data.internal.OAuth2WithdrawalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "gitHubUserClient", url = "https://api.github.com")
public interface GitHubUserClient {

  @GetMapping("/user")
  GitHubUserInfo getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);

  @DeleteMapping("/applications/{client_id}/grant")
  void withdrawal(
      @PathVariable("client_id") String clientId, @RequestBody OAuth2WithdrawalRequest request);
}
