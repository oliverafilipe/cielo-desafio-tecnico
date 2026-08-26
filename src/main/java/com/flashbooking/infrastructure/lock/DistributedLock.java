package com.flashbooking.infrastructure.lock;

import java.time.Duration;

public interface DistributedLock {
  boolean acquire(String key, String lockValue, Duration ttl);
  boolean release(String key, String lockValue);
}
