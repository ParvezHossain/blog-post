package com.parvez.blogs.service;

import com.parvez.blogs.dto.LoginRequest;
import com.parvez.blogs.dto.TokenResponse;
import com.parvez.blogs.dto.UserRegister;
import com.parvez.blogs.dto.UserResponse;
import com.parvez.blogs.entity.RefreshToken;
import com.parvez.blogs.entity.Role;
import com.parvez.blogs.entity.User;
import com.parvez.blogs.exception.InvalidTokenException;
import com.parvez.blogs.exception.ResourceNotFoundException;
import com.parvez.blogs.repository.RefreshTokenRepository;
import com.parvez.blogs.repository.UserRepository;
import com.parvez.blogs.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private LoginRequest dto;


    public UserResponse register(@NonNull UserRegister dto) {
        if (userRepository.existsByUsername(dto.username()))
            throw new DataIntegrityViolationException("Username is already in use");

        if (userRepository.existsByEmail(dto.email())) {
            throw new DataIntegrityViolationException("Email is already in use");
        }

        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setRole(dto.role() != null ? dto.role() : Role.USER);
        userRepository.save(user);

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    @Transactional
    public TokenResponse login(@NonNull LoginRequest dto) {
        this.dto = dto;
        // Remove old refresh token for same device
        rotateRefreshToken(dto.username());

        String accessToken = generateAccessToken(dto);
        String refreshToken = jwtUtil.generateRefreshToken(dto.username());
        saveRefreshToken(dto.username(), refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }

    private void rotateRefreshToken(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    private String generateAccessToken(@NonNull LoginRequest dto) {
        try {
            // 1. Find the user
            Optional<User> userOptional = userRepository.findByUsername(dto.username());

            // 2. Check if the user exists before doing anything else
            if (userOptional.isEmpty()) {
                throw new BadCredentialsException("Invalid username or password");
            }

            // 3. Extract the actual User object
            User user = userOptional.get();

            // 4. Validate password
            if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
                throw new BadCredentialsException("Invalid username or password");
            }

            // 5. Generate token using the actual user object's methods
            return jwtUtil.generateToken(user.getUsername(), String.valueOf(user.getRole()));

        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Username is already in use");
        }
    }

    private void saveRefreshToken(@NonNull String username, @NonNull String refreshToken) {
        RefreshToken token = RefreshToken
                .builder()
                .username(username)
                .token(refreshToken)
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(token);
    }

    public String extractToken(@NonNull String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid Authorization header");
        }

        return authHeader.substring(7);
    }

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

        saveRefreshToken(username, newRefreshToken);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
