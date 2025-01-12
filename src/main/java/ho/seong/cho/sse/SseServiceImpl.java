package ho.seong.cho.sse;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

  private static final Duration EMITTER_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration RECONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final String EMITTER_ID_FORMAT = "%s::%d";

  private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

  @Override
  public SseEmitter connect(SseType type, long userId) {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());

    this.emitterMap.put(EMITTER_ID_FORMAT.formatted(type, userId), emitter);

    emitter.onCompletion(() -> this.disconnect(emitter));
    emitter.onTimeout(() -> this.disconnect(emitter));
    return emitter;
  }

  @Override
  public void disconnect(SseType type, long userId) {
    String emitterId = EMITTER_ID_FORMAT.formatted(type, userId);
    SseEmitter emitter = this.emitterMap.get(emitterId);

    if (emitter != null) {
      emitter.complete();
      this.emitterMap.remove(emitterId);
    }
  }

  @Override
  public void disconnect(SseEmitter emitter) {
    this.emitterMap.entrySet().stream()
        .filter(entry -> entry.getValue().equals(emitter))
        .map(Map.Entry::getKey)
        .forEach(this.emitterMap::remove);
  }

  @Override
  public void broadcast(SseType type, Object data) {
    this.emitterMap.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(type.name()))
        .map(Map.Entry::getValue)
        .forEach(
            emitter -> {
              try {
                emitter.send(buildEvent(type, data));
              } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
              }
            });
  }

  @Override
  public void unicast(SseType type, long userId, Object data) {
    String emitterId = EMITTER_ID_FORMAT.formatted(type, userId);
    SseEmitter emitter = this.emitterMap.get(emitterId);

    if (emitter != null) {
      try {
        emitter.send(buildEvent(type, data));
      } catch (IOException | IllegalStateException ex) {
        emitter.completeWithError(ex);
      }
    }
  }

  private static SseEmitter.SseEventBuilder buildEvent(SseType type, Object data) {
    return SseEmitter.event()
        .id(UUID.randomUUID().toString())
        .name(type.name())
        .data(data)
        .reconnectTime(RECONNECT_TIMEOUT.toMillis());
  }
}
