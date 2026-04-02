package com.parvez.blogs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(1)
public class ConfigValidator implements CommandLineRunner {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void run(String... args) throws Exception {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new Exception("JWT secret is empty");
        }
        log.info("JWT config OK");
    }
}
