package ho.seong.cho.security.filter.support.impl;

import ho.seong.cho.security.annotation.RateLimit;
import ho.seong.cho.security.filter.support.RateLimiter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** {@link ConcurrentHashMap} 기반의 단순한 {@link RateLimiter} 구현체 */
public class SimpleRateLimiter implements RateLimiter {

  private final Map<String, RateLimitState> stateMap = new ConcurrentHashMap<>();

  @Override
  public void increment(String key) {
    RateLimitState state = this.stateMap.computeIfAbsent(key, k -> new RateLimitState());
    state.counter.incrementAndGet();
  }

  @Override
  public boolean isExceeded(String key, RateLimit rateLimit) {
    RateLimitState state = stateMap.computeIfAbsent(key, k -> new RateLimitState());

    long currentTime = System.currentTimeMillis();
    if (currentTime > state.resetTime) {
      resetState(state, rateLimit);
    }

    return state.counter.get() > rateLimit.maxRequests();
  }

  private void resetState(RateLimitState state, RateLimit rateLimit) {
    state.counter.set(0);
    long durationMillis = rateLimit.durationUnit().toMillis(rateLimit.duration());
    state.resetTime = (System.currentTimeMillis() + durationMillis);
  }

  /** 내부 상태를 관리하는 클래스 */
  private static class RateLimitState {

    private final AtomicInteger counter = new AtomicInteger(0);

    private volatile long resetTime;
  }
}
