package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.OAuth2Properties;
import ho.seong.cho.oauth.data.token.NaverOAuth2TokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "naverOAuth2Client", url = "https://nid.naver.com")
public interface NaverOAuth2Client {

  /**
   * 네이버 OAuth2 접근 토큰 발급
   *
   * @param grantType 인가 유형 / {@link OAuth2Properties.Naver#GRANT_TYPE_ISSUE}로 고정
   * @param clientId 애플리케이션 클라이언트 ID
   * @param clientSecret 애플리케이션 클라이언트 시크릿
   * @param redirectUri 애플리케이션 리다이렉트 URI
   * @param code OAuth2 인증 코드
   * @param state OAuth2 상태 토큰
   * @return 네이버 OAuth2 접근 토큰
   */
  @PostMapping("/oauth2.0/token")
  NaverOAuth2TokenDto issueToken(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code,
      @RequestParam("state") String state);

  /**
   * 네이버 OAuth2 사용자 연결 해제
   *
   * @param grantType 인가 유형 / {@link OAuth2Properties.Naver#GRANT_TYPE_WITHDRAWAL}로 고정
   * @param clientId 애플리케이션 클라이언트 ID
   * @param clientSecret 애플리케이션 클라이언트 시크릿
   * @param accessToken OAuth2 접근 토큰
   * @param serviceProvider 서비스 제공자 / {@code NAVER}로 고정
   */
  @PostMapping("/oauth2.0/token")
  void withdrawal(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("access_token") String accessToken,
      @RequestParam("service_provider") String serviceProvider);
}
