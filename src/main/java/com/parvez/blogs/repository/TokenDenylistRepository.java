package com.parvez.blogs.repository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@Transactional
@RequiredArgsConstructor
public class TokenDenylistRepository {

    private final StringRedisTemplate redisTemplate;

    public void save(String token, long ttlInMilliseconds) {
        redisTemplate.opsForValue().set(
                token,
                "revoked",
                Duration.ofMillis(ttlInMilliseconds)
        );
    }
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}
