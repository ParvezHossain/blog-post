package com.parvez.blogs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        // Fast lookup by token string on every reset attempt
        @Index(name = "idx_prt_token", columnList = "token", unique = true),
        // Fast cleanup of all tokens for a user on password change
        @Index(name = "idx_prt_username", columnList = "username")
})
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant expirationDate;

    public boolean isExpired() {
        return Instant.now().isAfter(expirationDate);
    }
}
