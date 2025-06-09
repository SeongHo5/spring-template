package ho.seong.cho.infra.client.sse.impl;

import ho.seong.cho.infra.client.sse.SseEmitterRepository;
import ho.seong.cho.sse.SseEmitterWrapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Repository;

/**
 * In-Memory {@link SseEmitterRepository} 구현체
 *
 * @apiNote 이 저장소는 <strong>영속성을 제공하지 않으며</strong>, 재기동 시 모든 데이터가 소실됩니다. 단일 인스턴스 환경에서만 사용하세요.
 * @implNote 이 클래스는 {@link ConcurrentHashMap}로 구현하여 Thread-Safe하게 사용할 수 있습니다.
 */
@Repository
public class SimpleSseEmitterRepository implements SseEmitterRepository {

  private final Map<Long, SseEmitterWrapper> emitterMap = new ConcurrentHashMap<>();

  @Override
  public <S extends SseEmitterWrapper> S save(S entity) {
    this.emitterMap.put(entity.id(), entity);
    return entity;
  }

  @Override
  public <S extends SseEmitterWrapper> List<S> saveAll(Iterable<S> entities) {
    return StreamSupport.stream(entities.spliterator(), false).map(this::save).toList();
  }

  @Override
  public Optional<SseEmitterWrapper> findById(Long id) {
    return Optional.of(id).map(emitterMap::get);
  }

  @Override
  public boolean existsById(Long id) {
    return this.emitterMap.containsKey(id);
  }

  @Override
  public List<SseEmitterWrapper> findAll() {
    return this.emitterMap.values().stream().toList();
  }

  @Override
  public List<SseEmitterWrapper> findAllById(Iterable<Long> ids) {
    return StreamSupport.stream(ids.spliterator(), false)
        .filter(Objects::nonNull)
        .map(this.emitterMap::get)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public long count() {
    return this.emitterMap.size();
  }

  @Override
  public void deleteById(Long id) {
    this.emitterMap.remove(id);
  }

  @Override
  public void delete(SseEmitterWrapper entity) {
    this.emitterMap.remove(entity.id());
  }

  @Override
  public void deleteAllById(Iterable<? extends Long> ids) {
    ids.forEach(this::deleteById);
  }

  @Override
  public void deleteAll(Iterable<? extends SseEmitterWrapper> entities) {
    entities.forEach(entity -> this.emitterMap.remove(entity.id()));
  }

  @Override
  public void deleteAll() {
    this.emitterMap.clear();
  }
}
