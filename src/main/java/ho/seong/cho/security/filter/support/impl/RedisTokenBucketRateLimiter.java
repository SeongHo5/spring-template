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
        -- 요청 식별자 Key
        local key = KEYS[1]

        -- 버킷의 최대 크기(최대 저장 가능한 토큰 수)
        local capacity = tonumber(ARGV[1])

        -- 제한 시간 간격(ms)
        local duration = tonumber(ARGV[2])

        -- 이번 요청에 사용할 토큰 수
        local requested = 1

        -- 현재 시간
        local now = tonumber(ARGV[3])

        -- 현재 남은 토큰 수와, 마지막으로 토큰을 충전한 시간을 조회
        local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
        local current_tokens = tonumber(bucket[1]) or capacity
        local last_refill = tonumber(bucket[2]) or now

        -- 토큰이 충분하면 사용
        if current_tokens >= requested then
            current_tokens = current_tokens - requested
            redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', last_refill)
            redis.call('EXPIRE', key, duration / 1000)
            return 1
        end

        -- 토큰이 부족하면 토큰을 채워넣고, 다시 확인
        local elapsed_time = now - last_refill
        local refill_rate = capacity / duration
        local new_tokens = math.min(capacity, math.floor(current_tokens + (elapsed_time * refill_rate)))

        if new_tokens >= requested then
            new_tokens = new_tokens - requested
            redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', now)
            redis.call('EXPIRE', key, duration / 1000)
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
