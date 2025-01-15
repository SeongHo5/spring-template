package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.data.entity.NaverUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "naverUserClient", url = "https://openapi.naver.com/v1/nid")
public interface NaverUserClient {

  /**
   * 네이버 사용자 정보 조회
   *
   * @param accessToken 네이버 OAuth2 액세스 토큰
   * @return 네이버 사용자 정보
   */
  @GetMapping("/me")
  NaverUserInfo getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);
}
