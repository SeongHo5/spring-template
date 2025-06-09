package ho.seong.cho.sse.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ho.seong.cho.infra.client.sse.SseEmitterRepository;
import ho.seong.cho.sse.SseEmitterWrapper;
import ho.seong.cho.sse.SseEvent;
import ho.seong.cho.sse.SseService;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

  private static final Duration EMITTER_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration RECONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final SseEmitterRepository repository;

  @Override
  public SseEmitter connect(final long userId) {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());

    emitter.onCompletion(() -> this.disconnect(userId));
    emitter.onTimeout(() -> this.disconnect(userId));

    sendConnectSuccess(emitter);
    this.repository.save(SseEmitterWrapper.of(userId, emitter));
    return emitter;
  }

  @Override
  public void disconnect(final long userId) {
    this.repository
        .findById(userId)
        .ifPresent(
            emitterWrapper -> {
              emitterWrapper.emitter().complete();
              this.repository.delete(emitterWrapper);
            });
  }

  @Override
  public void broadcast(SseEvent event) {
    this.repository.findAll().stream()
        .map(SseEmitterWrapper::emitter)
        .forEach(
            emitter -> {
              try {
                emitter.send(buildEvent(event));
              } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
              }
            });
  }

  @Override
  public void unicast(final long userId, SseEvent event) {
    this.repository
        .findById(userId)
        .map(SseEmitterWrapper::emitter)
        .ifPresent(
            emitter -> {
              try {
                emitter.send(buildEvent(event));
              } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
              }
            });
  }

  private static SseEmitter.SseEventBuilder buildEvent(SseEvent event)
      throws JsonProcessingException {
    return SseEmitter.event()
        .id(UUID.randomUUID().toString())
        .name(event.getEventType().name())
        .data(MAPPER.writeValueAsString(event))
        .reconnectTime(RECONNECT_TIMEOUT.toMillis());
  }

  private static void sendConnectSuccess(SseEmitter emitter) {
    try {
      emitter.send(SseEmitter.event().name("connected"));
    } catch (IOException ex) {
      emitter.completeWithError(ex);
    }
  }
}
