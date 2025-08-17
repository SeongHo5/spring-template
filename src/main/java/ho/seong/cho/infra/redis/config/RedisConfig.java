package ho.seong.cho.infra.redis.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.web.util.UriTemplate;

@Configuration
@EnableRedisRepositories(basePackages = "ho.seong.cho.infra.redis")
@RequiredArgsConstructor
public class RedisConfig {

  private final RedisProperties redisProperties;

  @Bean
  public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer()
        .setAddress(this.redisProperties.address().toString())
        .setPassword(this.redisProperties.password());
    return Redisson.create(config);
  }

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
    final int port = this.redisProperties.port();
    var configuration = new RedisStandaloneConfiguration(host, port);
    return new LettuceConnectionFactory(configuration);
  }
}
