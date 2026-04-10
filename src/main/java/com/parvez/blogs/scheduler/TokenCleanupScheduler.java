package com.parvez.blogs.scheduler;

import com.parvez.blogs.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    // Runs every hour — removes expired reset tokens silently
    @Transactional
    @Scheduled(fixedRateString = "${app.cleanup.interval-ms:3600000}")
    public void cleanupTokens() {
        passwordResetTokenRepository.deleteAllExpiredBefore(Instant.now());
        System.out.println("Expired password reset tokens purged");
        log.debug("Expired password reset tokens purged");
    }
}
