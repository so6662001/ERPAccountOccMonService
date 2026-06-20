package com.puxun.monitor.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Redis 的分布式锁，防止多实例重复执行调度任务。
 * Redis 不可用时 fail-open（记录告警并放行），避免阻塞业务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate redis;
    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

    public boolean tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                tokens.put(key, token);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("分布式锁不可用，放行 key={}: {}", key, e.getMessage());
            return true; // fail-open
        }
    }

    public void unlock(String key) {
        try {
            String token = tokens.remove(key);
            if (token != null && token.equals(redis.opsForValue().get(key))) {
                redis.delete(key);
            }
        } catch (Exception e) {
            log.debug("释放分布式锁失败 key={}: {}", key, e.getMessage());
        }
    }
}
