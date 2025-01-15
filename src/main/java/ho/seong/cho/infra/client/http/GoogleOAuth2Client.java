package ho.seong.cho.infra.client.http;

import ho.seong.cho.oauth.OAuth2Properties;
import ho.seong.cho.oauth.data.token.GoogleOAuth2TokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "googleOAuth2Client", url = "https://accounts.google.com/o/oauth2")
public interface GoogleOAuth2Client {

  /**
   * Google OAuth2 토큰 발급
   *
   * @param grantType 인가 유형 / {@link OAuth2Properties.Google#GRANT_TYPE} 로 고정
   * @param clientId 클라이언트 ID
   * @param clientSecret 클라이언트 비밀
   * @param redirectUri 리다이렉트 URI
   * @param code 인증 코드
   * @return OAuth2 토큰
   */
  @PostMapping("/token")
  GoogleOAuth2TokenDto issueToken(
      @RequestParam("grant_type") String grantType,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code);

  /**
   * Google OAuth2 연결 해제
   *
   * @param token OAuth2 토큰
   */
  @PostMapping("/revoke")
  void revokeToken(@RequestParam("token") String token);
}
