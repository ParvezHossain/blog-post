package com.parvez.blogs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    // Define maximum allowed requests per minute
    @Value("${spring.cache.data.redis.max_request_per_minute}")
    private int maxRequests = 10;

    @Value("${spring.cache.data.redis.window_second}")
    private int windowSeconds = 60;

    /**
     * Checks if the given key (IP + endpoint) is allowed.
     */
    public boolean isRequestAllowed(String key) {
        long now = Instant.now().getEpochSecond();

        requestCounts.compute(key, (k, info) -> {
            if (info == null || now - info.windowStart >= windowSeconds) {
                // New window
                return new RequestInfo(now, new AtomicInteger(1));
            } else {
                info.count.incrementAndGet();
                return info;
            }
        });

        return requestCounts.get(key).count.get() <= maxRequests;
    }

    private static class RequestInfo {
        long windowStart;
        AtomicInteger count;

        RequestInfo(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}