package com.parvez.blogs.service;

import com.parvez.blogs.config.AuthProvider;
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
import com.parvez.blogs.repository.TokenDenylistRepository;
import com.parvez.blogs.repository.UserRepository;
import com.parvez.blogs.security.JwtUtil;
import com.parvez.blogs.validation.LocalUserValidation;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenDenylistRepository tokenDenylistRepository;
    private final AuthenticationManager authenticationManager;
    private LoginRequest dto;
    private final Validator validator;


    public UserResponse register(@NonNull UserRegister dto) {
        if (userRepository.existsByUsername(dto.username()))
            throw new DataIntegrityViolationException("Username is already in use");

        // Prevent re-registration over an existing OAuth2 account
        userRepository.findByEmail(dto.email()).ifPresent(existing -> {
            if (existing.getProvider() != AuthProvider.LOCAL) {
                throw new DataIntegrityViolationException(
                        "This email is linked to a " + existing.getProvider() + " account. Please sign in with Google."
                );
            }
            throw new DataIntegrityViolationException("Email is already in use");
        });

        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setRole(dto.role() != null ? dto.role() : Role.USER);
        user.setProvider(AuthProvider.LOCAL);

        // Explicitly validate LOCAL-only constraints before saving
        Set<ConstraintViolation<User>> violations =
                validator.validate(user, LocalUserValidation.class);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

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

    public void logout(String authHeader) {
        String token = extractToken(authHeader);
        String username = jwtUtil.extractUsername(token);
        long ttl = this.getRemainingExpirationTime(token);
        if (ttl > 0) {
            // Store in Redis with the token as the key
            // Set the TTL so it auto-deletes when the JWT naturally expires
            tokenDenylistRepository.save(token, ttl);
        }
        refreshTokenRepository.deleteByUsername(username);
    }

    public void rotateRefreshToken(String username) {
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

    public void saveRefreshToken(@NonNull String username, @NonNull String refreshToken) {
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



    private long getRemainingExpirationTime(@NonNull String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtUtil.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            long diff = expiration.getTime() - System.currentTimeMillis();
            return Math.max(diff, 0);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }
}
