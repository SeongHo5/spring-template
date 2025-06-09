package ho.seong.cho.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE(Server-Sent Evennts)를 관리하는 서비스 인터페이스 */
public interface SseService {

  /**
   * 특정 사용자와 연결한다.
   *
   * @param type 이벤트 유형
   * @param userId 사용자 ID
   * @return 생성된 {@link SseEmitter} 객체
   */
  SseEmitter connect(long userId);

  /**
   * 특정 사용자와 연결을 종료한다.
   *
   * @param type 이벤트 유형
   * @param userId 사용자 ID
   */
  void disconnect(long userId);

  /**
   * 특정 이벤트 유형을 구독 중인 모든 사용자에게 이벤트 데이터를 전송한다.
   *
   * @param type 이벤트 유형
   * @param data 전송할 데이터
   */
  void broadcast(SseEvent event);

  /**
   * 특정 사용자에게 특정 이벤트 유형을 구독 중인 사용자에게 이벤트 데이터를 전송한다.
   *
   * @param type 이벤트 유형
   * @param userId 사용자 ID
   */
  void unicast(long userId, SseEvent event);
}
