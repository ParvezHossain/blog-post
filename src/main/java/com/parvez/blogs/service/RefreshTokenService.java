package com.parvez.blogs.service;

import com.parvez.blogs.dto.TokenResponse;
import com.parvez.blogs.entity.RefreshToken;
import com.parvez.blogs.entity.User;
import com.parvez.blogs.exception.InvalidTokenException;
import com.parvez.blogs.exception.ResourceNotFoundException;
import com.parvez.blogs.repository.RefreshTokenRepository;
import com.parvez.blogs.repository.UserRepository;
import com.parvez.blogs.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    // Grace Period: We can implement such logic later on.
    @Transactional
    public TokenResponse refreshRotation(@NonNull String oldRefreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> new InvalidTokenException("Token not recognized."));

        if (!jwtUtil.validateRefreshToken(storedToken.getToken())) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidTokenException("Invalid refresh token");
        }

        String username = jwtUtil.extractUsername(storedToken.getToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Username not found"));

        // ROTATION: Delete the used refresh token so it can't be used again
        refreshTokenRepository.delete(storedToken);

        String newAccessToken = jwtUtil.generateToken(username, user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        authService.saveRefreshToken(username, newRefreshToken);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

}
