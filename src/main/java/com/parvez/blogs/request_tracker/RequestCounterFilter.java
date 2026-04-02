package com.parvez.blogs.request_tracker;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestCounterFilter implements Filter {

    private static final AtomicLong REQUEST_COUNT = new AtomicLong(0);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Long requestCount = REQUEST_COUNT.incrementAndGet();

        HttpServletRequest req = (HttpServletRequest) request;
/*
        System.out.println(
                STR."[\{requestCount}] \{req.getMethod()} \{req.getRequestURI()}"
        );
*/

        chain.doFilter(request, response);
    }

    public static Long getRequestCount() {
        return REQUEST_COUNT.get();
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}