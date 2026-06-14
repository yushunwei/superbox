package com.superbox.config;

import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "rate_limit:chat:";

    public void checkChatLimit(Long userId) {
        String key = KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > 30) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试（每分钟30次限制）");
        }
    }
}
