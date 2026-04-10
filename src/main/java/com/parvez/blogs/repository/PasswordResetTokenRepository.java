package com.parvez.blogs.repository;

import com.parvez.blogs.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUsernameAndToken(String username, String token);

    Optional<PasswordResetToken> findByToken(String token);

    // Clean up all tokens for a user when password changes
    void deleteByUsername(String username);

    // Scheduled cleanup of expired tokens — keeps the table lean
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expirationDate < :now")
    void deleteAllExpiredBefore(Instant now);
}
