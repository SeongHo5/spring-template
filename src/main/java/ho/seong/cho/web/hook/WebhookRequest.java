package ho.seong.cho.web.hook;

/**
 * Discord Webhook 요청 DTO
 *
 * @param content 메시지 내용
 */
public record WebhookRequest(String content) {}
