package ho.seong.cho.concurrent;

import ho.seong.cho.utils.SpelEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAdvice {

  private static final String REDISSON_KEY_PREFIX = "API_LOCK::";

  private final RedissonClient redissonClient;

  @Around("@annotation(distributedLock)")
  public Object applyDistributedLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
      throws Throwable {
    String method = joinPoint.getSignature().toShortString();
    String key = generateKeyFromSpEL(joinPoint, distributedLock);
    RLock lock = this.redissonClient.getLock(key);

    log.info("Try acquiring lock for key='{}' | method='{}'", key, method);
    try {
      if (!lock.tryLock(
          distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit())) {
        log.warn("Failed to acquire lock key='{}' | method='{}'", key, method);
        return false;
      }
      log.info("Lock acquired for key='{}'", key);
      return joinPoint.proceed();
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
        log.info("Lock released for key='{}'", key);
      }
    }
  }

  private static String generateKeyFromSpEL(
      ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
    String key =
        SpelEngine.getDynamicValue(
                distributedLock.keyName(),
                ((MethodSignature) joinPoint.getSignature()).getParameterNames(),
                joinPoint.getArgs())
            .toString();
    return REDISSON_KEY_PREFIX + key;
  }

}
