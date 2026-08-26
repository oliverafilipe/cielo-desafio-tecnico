package com.flashbooking.infrastructure.lock;

import java.time.Duration;
import java.util.Collections;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisDistributedLockService implements DistributedLock {

  private static final String UNLOCK_LUA_SCRIPT =
      "if redis.call('get', KEYS[1]) == ARGV[1] then "
          + "return redis.call('del', KEYS[1]) "
          + "else "
          + "return 0 "
          + "end";

  private final StringRedisTemplate redisTemplate;
  private final DefaultRedisScript<Long> unlockScript;

  public RedisDistributedLockService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
    this.unlockScript = new DefaultRedisScript<>();
    this.unlockScript.setScriptText(UNLOCK_LUA_SCRIPT);
    this.unlockScript.setResultType(Long.class);
  }

  @Override
  public boolean acquire(String key, String lockValue, Duration ttl) {
    try {
      Boolean success = redisTemplate.opsForValue()
          .setIfAbsent(key, lockValue, ttl);
      return Boolean.TRUE.equals(success);
    } catch (Exception e) {
      // In case Redis is unreachable in unit/local tests without Redis container
      return false;
    }
  }

  @Override
  public boolean release(String key, String lockValue) {
    try {
      Long result = redisTemplate.execute(
          unlockScript,
          Collections.singletonList(key),
          lockValue
      );
      return result != null && result > 0;
    } catch (Exception e) {
      return false;
    }
  }
}
