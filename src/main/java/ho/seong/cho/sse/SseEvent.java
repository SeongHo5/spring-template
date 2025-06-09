package ho.seong.cho.sse;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * SSE(Server-Sent-Event) 전송을 위한 이벤트 객체의 공통 인터페이스
 *
 * <p>각 이벤트는 {@link SseType}을 가지며, SSE "{@code event}"에 사용된다.
 */
public interface SseEvent {

  /**
   * 이 이벤트의 타입을 반환한다.
   *
   * @return 이벤트 타입
   */
  @JsonIgnore
  SseType getEventType();
}
