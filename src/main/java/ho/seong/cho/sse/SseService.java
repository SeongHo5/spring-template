package ho.seong.cho.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {

  SseEmitter connect(SseType type, long userId);

  void disconnect(SseType type, long userId);

  void disconnect(SseEmitter emitter);

  void broadcast(SseType type, Object data);

  void unicast(SseType type, long userId, Object data);
}
