package com.parvez.blogs.serviceImp;

import com.parvez.blogs.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@ConditionalOnProperty(name = "rate-limiter.type", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisRateLimiterService implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.cache.data.redis.max-requests:10}")
    private int maxRequests;

    @Value("${spring.cache.data.redis.window-seconds:60}")
    private int windowSeconds;

    private static final String LUA_SCRIPT =
            "local current = redis.call('incr', KEYS[1]) " +
                    "if tonumber(current) == 1 then " +
                    "    redis.call('expire', KEYS[1], ARGV[2]) " +
                    "end " +
                    "return tonumber(current)";

    @Override
    public boolean isRequestAllowed(String key) {

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

        Long currentCount = redisTemplate.execute(script,
                Collections.singletonList("rate_limit:" + key),
                String.valueOf(maxRequests),
                String.valueOf(windowSeconds));

        return currentCount != null && currentCount <= maxRequests;
    }
}
