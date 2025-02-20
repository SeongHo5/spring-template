package ho.seong.cho.security.filter.support.impl;

import ho.seong.cho.security.annotation.RateLimit;
import ho.seong.cho.security.filter.support.RateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisTokenBucketRateLimiter implements RateLimiter {

  /** 토큰 버킷 알고리즘을 구현한 Lua 스크립트 */
  private static final String TOKEN_BUCKET_LUA_SCRIPT =
      """
        local key = KEYS[1]
        local rate = tonumber(ARGV[1])
        local capacity = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])
        local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
        local tokens = tonumber(bucket[1]) or capacity
        local last_refill = tonumber(bucket[2]) or now

        -- 시간 경과에 따른 토큰 충전
        local elapsed_time = now - last_refill
        local new_tokens = math.floor(elapsed_time / 1000 * rate)
        tokens = math.min(capacity, tokens + new_tokens)

        -- 토큰 사용
        if tokens >= requested then
            tokens = tokens - requested
            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
            redis.call('EXPIRE', key, math.ceil(capacity / rate) + 1)
            return 1
        else
            return 0
        end
        """;

  private final StringRedisTemplate redisTemplate;

  @Override
  public boolean isExceeded(String key, RateLimit rateLimit) {
    return !this.tryConsume(key, rateLimit);
  }

  @Override
  public boolean tryConsume(String key, RateLimit rateLimit) {
    var now = System.currentTimeMillis();
    var duration = Duration.of(rateLimit.duration(), rateLimit.durationUnit()).toSeconds();
    var script = new DefaultRedisScript<>(TOKEN_BUCKET_LUA_SCRIPT, Long.class);

    var result =
        this.redisTemplate.execute(
            script,
            List.of(key),
            String.valueOf(rateLimit.maxRequests()),
            String.valueOf(duration),
            String.valueOf(now),
            "1");
    return result != null && result == 1;
  }

  @Override
  public int getRemainingTokens(String key) {
    return Optional.of(key)
        .map(k -> this.redisTemplate.opsForHash().get(k, "tokens"))
        .map(Object::toString)
        .map(Integer::parseInt)
        .orElse(0);
  }

  @Override
  public void reset(String key, RateLimit rateLimit) {
    var hashOperations = this.redisTemplate.opsForHash();
    hashOperations.put(key, "tokens", rateLimit.maxRequests());
    hashOperations.put(key, "last_refill", String.valueOf(System.currentTimeMillis()));
  }
}
