package com.parvez.blogs.serviceImp;

import com.parvez.blogs.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ConditionalOnProperty(name = "rate-limiter.type", havingValue = "memory")
@RequiredArgsConstructor
public class InMemoryRateLimiterService implements RateLimiterService {
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    @Value("${rate-limiter.max-requests}")
    private int maxRequests = 10;

    @Value("${rate-limiter.window-seconds}")
    private int windowSeconds = 60;

    /**
     * Checks if the given key (IP + endpoint) is allowed.
     */
    @Override
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
