package ho.seong.cho.sse;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record SseEmitterWrapper(long id, SseEmitter emitter) implements Serializable {

  @Serial private static final long serialVersionUID = 2025060901L;

  public static SseEmitterWrapper of(long userId, SseEmitter emitter) {
    return new SseEmitterWrapper(userId, emitter);
  }
}
