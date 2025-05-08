package ho.seong.cho.web.hook;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Webhook 관련 Properties 관리 클래스
 *
 * @param discord Discord Webhook 관련 설정
 */
@ConfigurationProperties("service.webhook")
public record WebhookProperties(Discord discord) {

  /**
   * Discord Webhook 관련 설정
   *
   * @param serverId 서버 ID
   * @param token Webhook 토큰
   */
  public record Discord(String serverId, String token) {}
}
