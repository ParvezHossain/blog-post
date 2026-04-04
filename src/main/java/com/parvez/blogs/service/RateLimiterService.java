package com.parvez.blogs.service;

public interface RateLimiterService {
    boolean isRequestAllowed(String key);
}
