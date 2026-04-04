package com.parvez.blogs.config;

import com.parvez.blogs.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${rate-limiter.type}")
    private String type;

    private final RateLimiterService rateLimiterService;

    // A thread-safe map to store the number of requests per client IP
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    //List of endpoints to be rate-limited
    private final Map<String, List<String>> rateLimitedEndpoints = Map.of(
            ApiPaths.POSTS, List.of(HttpMethod.GET.name())
    );


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (rateLimitedEndpoints.containsKey(uri) && rateLimitedEndpoints.get(uri).contains(method)) {

            String clientIp = getClientIp(request);
            String key = clientIp + ":" + uri;
            System.out.println("key: " + key);

            if (!rateLimiterService.isRequestAllowed(key)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().println("TOO_MANY_REQUESTS");
                log.warn("Rate limit exceeded for IP {} on endpoint {}", clientIp, uri);
                return;
            }
        }

        System.out.println("rateLimitedEndpoints" + rateLimitedEndpoints);
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            xfHeader = request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
