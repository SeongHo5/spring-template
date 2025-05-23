package ho.seong.cho.infra.redis.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "ho.seong.cho.infra.redis")
@RequiredArgsConstructor
public class RedisConfig {

  private final RedisProperties redisProperties;

  @Bean
  public RedisTemplate<byte[], byte[]> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    redisTemplate.setEnableTransactionSupport(true);
    return redisTemplate;
  }

  /** Lecttuce 클라이언트 Connection Factory 설정 */
  @Bean
  protected RedisConnectionFactory redisConnectionFactory() {
    final String host = this.redisProperties.host();
    final Integer port = this.redisProperties.port();
    var configuration = new RedisStandaloneConfiguration(host, port);
    return new LettuceConnectionFactory(configuration);
  }
}
