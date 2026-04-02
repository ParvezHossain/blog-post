package com.parvez.blogs.request_tracker;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
public class MethodInvocationCounterAspect {

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Around("execution(* com.parvez.blogs.service..*(..))")
    public Object countServiceCalls(ProceedingJoinPoint pjp) throws Throwable {

        String method = pjp.getSignature().toShortString();

        counters
                .computeIfAbsent(method, k -> new AtomicLong(0))
                .incrementAndGet();

        return pjp.proceed();
    }

    public long getCount(String method) {
        return counters.getOrDefault(method, new AtomicLong(0)).get();
    }
}