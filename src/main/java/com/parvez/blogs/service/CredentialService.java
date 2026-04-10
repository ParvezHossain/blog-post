package com.parvez.blogs.service;

import com.parvez.blogs.amqp.EmailProducer;
import com.parvez.blogs.dto.EmailEvent;
import com.parvez.blogs.entity.PasswordResetToken;
import com.parvez.blogs.entity.User;
import com.parvez.blogs.exception.InvalidTokenException;
import com.parvez.blogs.exception.ResourceNotFoundException;
import com.parvez.blogs.repository.PasswordResetTokenRepository;
import com.parvez.blogs.repository.RefreshTokenRepository;
import com.parvez.blogs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CredentialService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailProducer emailProducer;
    private final PasswordEncoder passwordEncoder;

    // Externalised — no magic numbers buried in code
    @Value("${app.password-reset.expiry-minutes:10}")
    private int resetExpiryMinutes;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            refreshTokenRepository.deleteByUsername(user.getUsername());
            passwordResetTokenRepository.deleteByUsername(user.getUsername());

            String rawToken = UUID.randomUUID().toString();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(rawToken);
            resetToken.setUsername(user.getUsername());
            resetToken.setExpirationDate(Instant.now().plus(resetExpiryMinutes, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);

            emailProducer.sendPasswordResetEmail(
                    user.getEmail(),
                    buildResetLink(rawToken)
            );

        });
    }

    @Transactional
    public void resetPassword(String password, String token) {

        PasswordResetToken passwordResetToken = passwordResetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or already-used reset token"));

        if (passwordResetToken.isExpired()){
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new InvalidTokenException("Reset token has expired. Please request a new one.");
        }

        User user = userRepository
                .findByUsername(passwordResetToken.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (passwordResetToken.getExpirationDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new ResourceNotFoundException("Token is expired!");
        }
        user.setPassword(passwordEncoder.encode(password));

        refreshTokenRepository.deleteByUsername(user.getUsername());
        log.info("Password successfully reset for user '{}'", user.getUsername());

    }

    private String buildResetLink(String token) {
        System.out.println("frontendUrl"  + frontendUrl);
        return frontendUrl + "/reset-password?token=" + token;
    }
}
