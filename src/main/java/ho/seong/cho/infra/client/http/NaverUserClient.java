package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.entity.NaverUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "naverUserClient", url = "https://openapi.naver.com/v1/nid")
public interface NaverUserClient {

  @GetMapping("/me")
  NaverUserInfo getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);
}
