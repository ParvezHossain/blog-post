package com.parvez.blogs.config;


import com.parvez.blogs.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.filters.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    // A thread-safe map to store the number of requests per client IP
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    // List of endpoints to be rate-limited
    private final Map<String, List<String>> rateLimitedEndpoints = Map.of(
            ApiPaths.FORGOT_PASSWORD, List.of(HttpMethod.POST.name()),
            ApiPaths.RESET_PASSWORD, List.of(HttpMethod.POST.name())
            // Add more endpoints here as needed
    );


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (
                rateLimitedEndpoints.containsKey(path)
                        && rateLimitedEndpoints.get(path).contains(method)
        ) {
            String key = request.getRemoteAddr() + ":" + path;
            if (!rateLimiterService.isRequestAllowed(key)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().println("TOO_MANY_REQUESTS");
                log.warn("Rate limit exceeded for IP {} on endpoint {}", request.getRemoteAddr(), path);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}