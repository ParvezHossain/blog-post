package com.parvez.blogs.dto;

import com.parvez.blogs.entity.Role;

import java.time.Instant;

public record UserRegister(
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        Role role) {
}
