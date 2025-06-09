package ho.seong.cho.infra.client.sse;

import ho.seong.cho.sse.SseEmitterWrapper;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SseEmitterRepository extends ListCrudRepository<SseEmitterWrapper, Long> {}
