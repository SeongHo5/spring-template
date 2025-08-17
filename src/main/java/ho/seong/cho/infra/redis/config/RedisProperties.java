package ho.seong.cho.infra.redis.config;

import jakarta.annotation.Nullable;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriTemplate;

/**
 * Redis 서버 설정 정보
 *
 * @param host 서버 호스트 주소
 * @param port 서버 포트
 * @param password 서버 비밀번호 <i>(nullable)</i>
 */
@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(String host, Integer port, @Nullable String password) {

  private static final UriTemplate REDIS_ADDRESS = new UriTemplate("redis://{host}:{port}");

  public URI address() {
    return REDIS_ADDRESS.expand(this.host, this.port);
  }
}
