package com.parvez.blogs.config;

import com.parvez.blogs.entity.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return Optional.ofNullable(username);
    }
}
