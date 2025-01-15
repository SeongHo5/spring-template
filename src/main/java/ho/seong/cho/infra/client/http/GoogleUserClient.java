package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.entity.GoogleUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "googleUserClient", url = "https://www.googleapis.com")
public interface GoogleUserClient {

  @GetMapping("/oauth2/v3/userinfo")
  GoogleUserInfo getUserInfo(@RequestParam("access_token") String accessToken);
}
