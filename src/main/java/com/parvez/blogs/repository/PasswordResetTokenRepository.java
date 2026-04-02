package com.parvez.blogs.repository;

import com.parvez.blogs.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUsernameAndToken(String username, String token);
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUsername(String username);
}
